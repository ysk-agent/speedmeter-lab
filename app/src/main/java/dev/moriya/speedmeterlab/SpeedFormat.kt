package dev.moriya.speedmeterlab

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Pure formatter for the speed meter.
 *
 * The display format is fixed to "XX.X MB/s": 1 MB = 1,000,000 bytes,
 * quantized to 0.1 MB/s steps. Quantization is deliberate — fluctuations
 * below 100 KB/s do not change the string, so the notification icon is
 * not redrawn for them (the service only updates the icon when the
 * string changes).
 */
object SpeedFormat {
    fun format(bytesPerSecond: Double): String {
        if (bytesPerSecond.isNaN() || bytesPerSecond <= 0.0) return "0.0 MB/s"
        val mb = bytesPerSecond / 1_000_000.0
        val quantized = (mb * 10.0).roundToLong() / 10.0
        return String.format(Locale.US, "%.1f MB/s", quantized)
    }
}
