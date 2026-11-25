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

        addPaddingSliderRow(root)

        addSpinnerRow(root, "Font weight",
            Prefs.FONT_WEIGHT_OPTIONS.map { "$it" },
            Prefs.FONT_WEIGHT_OPTIONS.indexOf(Prefs.fontWeight(this))
        ) { pos -> Prefs.setFontWeight(this, Prefs.FONT_WEIGHT_OPTIONS[pos]) }

        addStretchSliderRow(root)

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

    /**
     * Vertical stretch slider, x1.40 to x1.70 in 0.01 steps
     * (Moriya, 2026-11-26). The value label updates live while
     * dragging; the service reads the preference on every render,
     * and the icon re-render key includes it, so the status-bar
     * icon follows the slider even while the value is static.
     */
    private fun addStretchSliderRow(root: LinearLayout) {
        val labelView = TextView(this).apply {
            text = "Vertical stretch: " + Prefs.stretchLabel(Prefs.stretchHundredths(this@MainActivity))
            textSize = 16f
        }
        root.addView(labelView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 32 })
        val slider = android.widget.SeekBar(this).apply {
            max = Prefs.STRETCH_MAX_H - Prefs.STRETCH_MIN_H
            progress = Prefs.stretchHundredths(this@MainActivity) - Prefs.STRETCH_MIN_H
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    val value = Prefs.STRETCH_MIN_H + progress
                    Prefs.setStretchHundredths(this@MainActivity, value)
                    labelView.text = "Vertical stretch: " + Prefs.stretchLabel(value)
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            })
        }
        root.addView(slider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    /**
     * Icon padding slider, 0.0 to 5.0 percent in 0.1 steps
     * (Moriya, 2026-11-26). Same live behaviour as the stretch
     * slider: the icon re-renders as the value changes.
     */
    private fun addPaddingSliderRow(root: LinearLayout) {
        val labelView = TextView(this).apply {
            text = "Icon padding: " + Prefs.paddingLabel(Prefs.iconPaddingTenths(this@MainActivity))
            textSize = 16f
        }
        root.addView(labelView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 32 })
        val slider = android.widget.SeekBar(this).apply {
            max = Prefs.PADDING_MAX_TENTHS
            progress = Prefs.iconPaddingTenths(this@MainActivity)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    Prefs.setIconPaddingTenths(this@MainActivity, progress)
                    labelView.text = "Icon padding: " + Prefs.paddingLabel(progress)
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            })
        }
        root.addView(slider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
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
