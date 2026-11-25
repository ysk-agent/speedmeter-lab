package dev.moriya.speedmeterlab

import android.content.Context

/** SharedPreferences access shared by the activity and the service. */
object Prefs {
    private const val FILE = "speedmeter"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_SEC = "interval_sec"
    private const val KEY_SMOOTHING = "smoothing_ema"

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

    fun setIntervalSec(context: Context, sec: Int) {
        require(sec in INTERVALS_SEC) { "unsupported interval: $sec" }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_INTERVAL_SEC, sec).apply()
    }
}
