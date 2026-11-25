package dev.moriya.speedmeterlab

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Pure formatter for the speed meter.
 *
 * The display format is fixed to "X.XX MB/s": 1 MB = 1,000,000 bytes,
 * quantized to 0.01 MB/s steps (decided 2026-11-25, superseding the
 * earlier 0.1 MB/s format). Finer quantization means the string — and
 * therefore the notification icon — changes more often; that is the
 * accepted cost of the second decimal.
 */
object SpeedFormat {
    fun format(bytesPerSecond: Double): String {
        if (bytesPerSecond.isNaN() || bytesPerSecond <= 0.0) return "0.00 MB/s"
        val mb = bytesPerSecond / 1_000_000.0
        val quantized = (mb * 100.0).roundToLong() / 100.0
        return String.format(Locale.US, "%.2f MB/s", quantized)
    }
}
