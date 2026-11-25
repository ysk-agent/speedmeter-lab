package dev.moriya.speedmeterlab

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure formatter for the speed meter.
 *
 * The display is always in MB/s (1 MB = 1,000,000 bytes); the number
 * of decimals is configurable (0, 1 or 2) and the value is quantized
 * to that step, so the string — and therefore the notification icon —
 * changes only when the quantized value changes. Two decimals was the
 * settled default on 2026-11-25.
 */
object SpeedFormat {
    val DECIMALS_OPTIONS = intArrayOf(0, 1, 2)
    const val DEFAULT_DECIMALS = 2

    fun format(bytesPerSecond: Double, decimals: Int = DEFAULT_DECIMALS): String {
        require(decimals in DECIMALS_OPTIONS) { "unsupported decimals: $decimals" }
        if (bytesPerSecond.isNaN() || bytesPerSecond <= 0.0) {
            return String.format(Locale.US, "%.${decimals}f MB/s", 0.0)
        }
        val mb = bytesPerSecond / 1_000_000.0
        val scale = 10.0.pow(decimals)
        val quantized = (mb * scale).roundToLong() / scale
        return String.format(Locale.US, "%.${decimals}f MB/s", quantized)
    }
}
