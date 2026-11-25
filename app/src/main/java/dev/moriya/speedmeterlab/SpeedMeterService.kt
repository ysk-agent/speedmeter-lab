package dev.moriya.speedmeterlab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.TrafficStats
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock

/**
 * Foreground service that polls TrafficStats totals and shows the
 * current speed in the status bar.
 *
 * Power rules (the point of this lab app):
 *  - Polling runs only while the meter is switched on AND the screen
 *    is interactive. Screen off stops the handler entirely; on screen
 *    on the counter baseline is re-taken so traffic during the off
 *    period never shows up as a spike.
 *  - The notification icon is a bitmap rendered from the speed string,
 *    and the notification is re-posted only when the string changes.
 */
class SpeedMeterService : Service() {

    companion object {
        const val ACTION_APPLY = "dev.moriya.speedmeterlab.APPLY"
        private const val CHANNEL_ID = "speed_meter"
        private const val NOTIFICATION_ID = 1
    }

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var polling = false
    private var screenOn = true

    private var lastTotalBytes = -1L
    private var lastSampleNanos = 0L
    private var lastText: String? = null
    private var lastIconKey: String? = null
    private val smoother = SpeedSmoother()

    private val tick = object : Runnable {
        override fun run() {
            sample()
            val h = handler ?: return
            if (polling) h.postDelayed(this, Prefs.intervalSec(this@SpeedMeterService) * 1000L)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    stopPolling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    if (Prefs.isEnabled(this@SpeedMeterService)) startPolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        screenOn = pm.isInteractive
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // (Re)post the foreground notification, then (re)arm polling.
        startForeground(NOTIFICATION_ID, buildNotification(lastText ?: zeroText()))
        if (screenOn) startPolling() else stopPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        stopPolling()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered; nothing to do.
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        if (thread == null) {
            thread = HandlerThread("speed-meter").apply { start() }
            handler = Handler(thread!!.looper)
        }
        // Re-take the baseline so the first sample after (re)start is clean.
        lastTotalBytes = -1L
        lastText = null
        lastIconKey = null
        smoother.reset()
        polling = true
        handler?.removeCallbacks(tick)
        handler?.post(tick)
    }

    private fun stopPolling() {
        polling = false
        handler?.removeCallbacks(tick)
    }

    private fun sample() {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            updateText(zeroText())
            return
        }
        val total = rx + tx
        val now = SystemClock.elapsedRealtimeNanos()
        if (lastTotalBytes < 0L) {
            lastTotalBytes = total
            lastSampleNanos = now
            updateText(zeroText())
            return
        }
        val deltaBytes = total - lastTotalBytes
        val deltaSec = (now - lastSampleNanos) / 1_000_000_000.0
        lastTotalBytes = total
        lastSampleNanos = now
        if (deltaSec <= 0.0) return
        // A negative delta means the counter reset (e.g. reboot); the
        // baseline above has already been re-taken, so just skip.
        if (deltaBytes < 0L) return
        val rawBps = deltaBytes / deltaSec
        // The EMA is fed on every sample so toggling the switch
        // mid-run switches to an already-primed value.
        smoother.windowSamples = Prefs.emaWindow(this)
        val smoothedBps = smoother.update(rawBps)
        val displayBps = if (Prefs.isSmoothing(this)) smoothedBps else rawBps
        updateText(SpeedFormat.format(displayBps, Prefs.decimals(this)))
    }

    private fun zeroText(): String = SpeedFormat.format(0.0, Prefs.decimals(this))

    private fun updateText(text: String) {
        // The icon depends on the padding setting as well as the text,
        // so a padding change re-renders even when the value is static
        // (important in the lab, where padding is tuned while idle).
        val iconKey = text + "@" + Prefs.iconPaddingPercent(this)
        if (iconKey == lastIconKey) return
        lastIconKey = iconKey
        lastText = text
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
        // Keep the in-app display in sync while the activity is open.
        sendBroadcast(Intent(MainActivity.ACTION_SPEED_UPDATE).apply {
            setPackage(packageName)
            putExtra(MainActivity.EXTRA_SPEED_TEXT, text)
        })
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.graphics.drawable.Icon.createWithBitmap(renderIcon(text)))
            .setContentTitle(text)
            .setContentText("Speed Meter Lab")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * Renders the speed as a status-bar icon bitmap: the number only,
     * no unit (decided 2026-11-25 — the unit font-ratio setting went
     * away with it). Sizing and centring use the ink bounds
     * (Paint.getTextBounds), not the font metrics: the font's own
     * top/ascent/descent reserves would otherwise add invisible
     * padding on top of the configured one and shift the visual
     * centre. With ink bounds, the padding setting is the visual
     * padding — the number's ink touches the padding box exactly.
     */
    private fun renderIcon(text: String): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val number = text.removeSuffix(" MB/s")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val padPx = size * Prefs.iconPaddingPercent(this) / 100f
        val avail = size - 2 * padPx
        // Measure the ink box at a reference size, then scale so it
        // fits the available box exactly (ink scales linearly).
        val bounds = android.graphics.Rect()
        paint.textSize = 100f
        paint.getTextBounds(number, 0, number.length, bounds)
        val scale = minOf(
            avail / bounds.width().toFloat(),
            avail / bounds.height().toFloat()
        )
        paint.textSize = 100f * scale
        paint.getTextBounds(number, 0, number.length, bounds)
        // Draw so the ink box itself is centred: (left, top) of the
        // bounds are relative to the (x, y) draw origin.
        val x = size / 2f - (bounds.left + bounds.right) / 2f
        val y = size / 2f - (bounds.top + bounds.bottom) / 2f
        canvas.drawText(number, x, y, paint)
        return bitmap
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, "Speed meter", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Current network speed in the status bar"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }
}
