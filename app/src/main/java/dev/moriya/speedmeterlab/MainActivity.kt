package dev.moriya.speedmeterlab

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView

/**
 * Single-screen lab UI: an on/off switch, the sampling-interval
 * dropdown, and a large in-app readout of the current speed.
 *
 * The interval can be changed while the meter runs; the service reads
 * the preference on every tick, so a change takes effect on the next
 * sample without a restart or reinstall.
 */
class MainActivity : Activity() {

    companion object {
        const val ACTION_SPEED_UPDATE = "dev.moriya.speedmeterlab.SPEED_UPDATE"
        const val EXTRA_SPEED_TEXT = "speed_text"
        private const val REQ_NOTIFICATIONS = 1
    }

    private lateinit var speedView: TextView
    private lateinit var meterSwitch: Switch

    private val speedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra(EXTRA_SPEED_TEXT) ?: return
            speedView.text = text
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 48)
        }

        speedView = TextView(this).apply {
            text = SpeedFormat.format(0.0, Prefs.decimals(this@MainActivity))
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(speedView)

        meterSwitch = Switch(this).apply {
            text = "Speed meter"
            textSize = 20f
            isChecked = Prefs.isEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> onMeterToggled(checked) }
        }
        root.addView(meterSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 48 })

        val intervalLabel = TextView(this).apply {
            text = "Sampling interval"
            textSize = 16f
        }
        root.addView(intervalLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 48 })

        val labels = Prefs.INTERVALS_SEC.map { "$it sec" }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item, labels
            )
            val current = Prefs.INTERVALS_SEC.indexOf(Prefs.intervalSec(this@MainActivity))
            setSelection(if (current >= 0) current else 0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    Prefs.setIntervalSec(this@MainActivity, Prefs.INTERVALS_SEC[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(spinner)

        val smoothingSwitch = Switch(this).apply {
            text = "Smoothing (EMA)"
            textSize = 20f
            isChecked = Prefs.isSmoothing(this@MainActivity)
            setOnCheckedChangeListener { _, checked ->
                Prefs.setSmoothing(this@MainActivity, checked)
            }
        }
        root.addView(smoothingSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 48 })

        addSpinnerRow(root, "Decimals",
            SpeedFormat.DECIMALS_OPTIONS.map { "$it" },
            SpeedFormat.DECIMALS_OPTIONS.indexOf(Prefs.decimals(this))
        ) { pos -> Prefs.setDecimals(this, SpeedFormat.DECIMALS_OPTIONS[pos]) }

        addSpinnerRow(root, "EMA window (samples)",
            SpeedSmoother.WINDOW_OPTIONS.map { "$it" },
            SpeedSmoother.WINDOW_OPTIONS.indexOf(Prefs.emaWindow(this))
        ) { pos -> Prefs.setEmaWindow(this, SpeedSmoother.WINDOW_OPTIONS[pos]) }

        addSpinnerRow(root, "Icon padding (%)",
            Prefs.ICON_PADDING_OPTIONS.map { "$it%" },
            Prefs.ICON_PADDING_OPTIONS.indexOf(Prefs.iconPaddingPercent(this))
        ) { pos -> Prefs.setIconPaddingPercent(this, Prefs.ICON_PADDING_OPTIONS[pos]) }

        addSpinnerRow(root, "Font weight",
            Prefs.FONT_WEIGHT_OPTIONS.map { "$it" },
            Prefs.FONT_WEIGHT_OPTIONS.indexOf(Prefs.fontWeight(this))
        ) { pos -> Prefs.setFontWeight(this, Prefs.FONT_WEIGHT_OPTIONS[pos]) }

        addSpinnerRow(root, "Vertical stretch",
            Prefs.STRETCH_OPTIONS.map { Prefs.stretchLabel(it) },
            Prefs.STRETCH_OPTIONS.indexOf(Prefs.stretchScaleXPercent(this))
        ) { pos -> Prefs.setStretchScaleXPercent(this, Prefs.STRETCH_OPTIONS[pos]) }

        val installedVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: PackageManager.NameNotFoundException) {
            "?"
        }
        val versionView = TextView(this).apply {
            text = "Speed Meter Lab v$installedVersion"
            textSize = 14f
            gravity = Gravity.CENTER
        }
        root.addView(versionView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 64 })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(speedReceiver, IntentFilter(ACTION_SPEED_UPDATE),
            Context.RECEIVER_NOT_EXPORTED)
        meterSwitch.isChecked = Prefs.isEnabled(this)
        // Reconcile the persisted ON preference with the service's
        // actual state. Setting the switch above does not fire the
        // listener when the state is unchanged, so after an app
        // update killed the service the switch showed ON while
        // nothing ran. Start the service here if it should be on
        // but is not.
        if (Prefs.isEnabled(this) && !SpeedMeterService.running) {
            startForegroundService(Intent(this, SpeedMeterService::class.java))
        }
    }

    override fun onPause() {
        try {
            unregisterReceiver(speedReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered; nothing to do.
        }
        super.onPause()
    }

    private fun addSpinnerRow(
        root: LinearLayout,
        label: String,
        options: List<String>,
        currentIndex: Int,
        onSelect: (Int) -> Unit
    ) {
        val labelView = TextView(this).apply {
            text = label
            textSize = 16f
        }
        root.addView(labelView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 32 })
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item, options
            )
            setSelection(if (currentIndex >= 0) currentIndex else 0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    onSelect(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(spinner)
    }

    private fun onMeterToggled(checked: Boolean) {
        Prefs.setEnabled(this, checked)
        val intent = Intent(this, SpeedMeterService::class.java)
        if (checked) {
            requestNotificationPermissionIfNeeded()
            speedView.text = SpeedFormat.format(0.0, Prefs.decimals(this))
            startForegroundService(intent)
        } else {
            stopService(intent)
            speedView.text = SpeedFormat.format(0.0, Prefs.decimals(this))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        }
    }
}
