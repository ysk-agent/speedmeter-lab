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
    private const val KEY_PADDING_TENTHS = "icon_padding_tenths"

    /**
     * Icon padding in tenths of a percent, adjusted with a slider
     * over 0.0-5.0 percent (Moriya, 2026-11-26). Migrates the old
     * whole-percent spinner value (0/5/10/...) on first read,
     * clamped into the slider range.
     */
    const val PADDING_MAX_TENTHS = 50
    const val DEFAULT_PADDING_TENTHS = 25

    private const val KEY_FONT_WEIGHT = "font_weight"

    val FONT_WEIGHT_OPTIONS = intArrayOf(300, 400, 500, 600, 700, 800, 900)
    const val DEFAULT_FONT_WEIGHT = 700

    private const val KEY_STRETCH_H = "stretch_hundredths"

    /**
     * Vertical stretch as hundredths (140 = x1.40), adjusted with a
     * slider over 1.40-1.70 (Moriya, 2026-11-26 — this replaced the
     * Auto/fixed spinner after one round). The renderer converts it
     * to a horizontal scale: textScaleX = 100 / stretch, so x1.55
     * condenses the glyphs to about 65 percent width.
     */
    const val STRETCH_MIN_H = 140
    const val STRETCH_MAX_H = 170
    const val DEFAULT_STRETCH_H = 150

    fun stretchLabel(hundredths: Int): String =
        String.format("x%.2f", hundredths / 100f)

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

    /** Icon padding in tenths of a percent, 0 (0.0%) to 50 (5.0%). */
    fun iconPaddingTenths(context: Context): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_PADDING_TENTHS)) {
            val v = prefs.getInt(KEY_PADDING_TENTHS, DEFAULT_PADDING_TENTHS)
            return if (v in 0..PADDING_MAX_TENTHS) v else DEFAULT_PADDING_TENTHS
        }
        if (prefs.contains(KEY_ICON_PADDING)) {
            return (prefs.getInt(KEY_ICON_PADDING, 0) * 10)
                .coerceIn(0, PADDING_MAX_TENTHS)
        }
        return DEFAULT_PADDING_TENTHS
    }

    fun setIconPaddingTenths(context: Context, tenths: Int) {
        require(tenths in 0..PADDING_MAX_TENTHS)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PADDING_TENTHS, tenths).apply()
    }

    fun paddingLabel(tenths: Int): String =
        String.format("%.1f%%", tenths / 10f)

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

    /** Vertical stretch in hundredths, 140 (x1.40) to 170 (x1.70). */
    fun stretchHundredths(context: Context): Int {
        val v = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_STRETCH_H, DEFAULT_STRETCH_H)
        return if (v in STRETCH_MIN_H..STRETCH_MAX_H) v else DEFAULT_STRETCH_H
    }

    fun setStretchHundredths(context: Context, hundredths: Int) {
        require(hundredths in STRETCH_MIN_H..STRETCH_MAX_H)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_STRETCH_H, hundredths).apply()
    }

    fun setIntervalSec(context: Context, sec: Int) {
        require(sec in INTERVALS_SEC) { "unsupported interval: $sec" }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_SEC, sec).apply()
    }
}
