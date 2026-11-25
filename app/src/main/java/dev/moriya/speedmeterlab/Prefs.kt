package dev.moriya.speedmeterlab

import android.content.Context

/** SharedPreferences access shared by the activity and the service. */
object Prefs {
    private const val FILE = "speedmeter"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_SEC = "interval_sec"
    private const val KEY_SMOOTHING = "smoothing_ema"
    private const val KEY_DECIMALS = "decimals"
    private const val KEY_EMA_WINDOW = "ema_window"
    private const val KEY_ICON_PADDING = "icon_padding_percent"

    val ICON_PADDING_OPTIONS = intArrayOf(0, 5, 10, 15, 20, 25)
    const val DEFAULT_ICON_PADDING = 5

    private const val KEY_FONT_WEIGHT = "font_weight"

    val FONT_WEIGHT_OPTIONS = intArrayOf(300, 400, 500, 600, 700, 800, 900)
    const val DEFAULT_FONT_WEIGHT = 700

    private const val KEY_STRETCH = "stretch_scalex_percent"

    /**
     * Horizontal scale (textScaleX) in percent; 0 means Auto, where
     * the condensation is derived from the width overflow as before.
     * A fixed value of e.g. 70 condenses the glyphs to 70 percent
     * width, which reads as a vertical stretch of about 1.43x.
     */
    val STRETCH_OPTIONS = intArrayOf(0, 100, 90, 80, 70, 60, 50)
    const val DEFAULT_STRETCH = 0

    fun stretchLabel(scaleXPercent: Int): String =
        if (scaleXPercent == 0) "Auto"
        else "x" + String.format("%.2f", 100f / scaleXPercent).trimEnd('0').trimEnd('.')

    val INTERVALS_SEC = intArrayOf(1, 2, 3, 5, 10)
    const val DEFAULT_INTERVAL_SEC = 1

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun intervalSec(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL_SEC, DEFAULT_INTERVAL_SEC)
        return if (v in INTERVALS_SEC) v else DEFAULT_INTERVAL_SEC
    }

    fun isSmoothing(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SMOOTHING, false)

    fun setSmoothing(context: Context, on: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SMOOTHING, on).apply()
    }

    fun decimals(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_DECIMALS, SpeedFormat.DEFAULT_DECIMALS)
        return if (v in SpeedFormat.DECIMALS_OPTIONS) v else SpeedFormat.DEFAULT_DECIMALS
    }

    fun setDecimals(context: Context, decimals: Int) {
        require(decimals in SpeedFormat.DECIMALS_OPTIONS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DECIMALS, decimals).apply()
    }

    fun emaWindow(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_EMA_WINDOW, SpeedSmoother.DEFAULT_WINDOW)
        return if (v in SpeedSmoother.WINDOW_OPTIONS) v else SpeedSmoother.DEFAULT_WINDOW
    }

    fun setEmaWindow(context: Context, window: Int) {
        require(window in SpeedSmoother.WINDOW_OPTIONS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_EMA_WINDOW, window).apply()
    }

    fun iconPaddingPercent(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_ICON_PADDING, DEFAULT_ICON_PADDING)
        return if (v in ICON_PADDING_OPTIONS) v else DEFAULT_ICON_PADDING
    }

    fun setIconPaddingPercent(context: Context, percent: Int) {
        require(percent in ICON_PADDING_OPTIONS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ICON_PADDING, percent).apply()
    }

    fun fontWeight(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_FONT_WEIGHT, DEFAULT_FONT_WEIGHT)
        return if (v in FONT_WEIGHT_OPTIONS) v else DEFAULT_FONT_WEIGHT
    }

    fun setFontWeight(context: Context, weight: Int) {
        require(weight in FONT_WEIGHT_OPTIONS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_FONT_WEIGHT, weight).apply()
    }

    /** Horizontal scale percent, or 0 for Auto. */
    fun stretchScaleXPercent(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_STRETCH, DEFAULT_STRETCH)
        return if (v in STRETCH_OPTIONS) v else DEFAULT_STRETCH
    }

    fun setStretchScaleXPercent(context: Context, percent: Int) {
        require(percent in STRETCH_OPTIONS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_STRETCH, percent).apply()
    }

    fun setIntervalSec(context: Context, sec: Int) {
        require(sec in INTERVALS_SEC) { "unsupported interval: $sec" }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_SEC, sec).apply()
    }
}
