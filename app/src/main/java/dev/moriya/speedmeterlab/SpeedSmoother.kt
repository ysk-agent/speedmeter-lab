package dev.moriya.speedmeterlab

/**
 * Exponential moving average over the raw bytes-per-second samples.
 *
 * The strength is expressed as an effective window length N in
 * samples, converted with the standard relation alpha = 2 / (N + 1).
 * The settled default N = 4 gives alpha 0.4.
 *
 * The decay is asymmetric (Moriya, 2026-11-27: the symmetric decay
 * felt too slow). Rising uses alpha as-is, so a spike's attack is
 * unchanged (0, 3.8, 0 still peaks at 1.52). Falling squares the
 * retention — (1 - alpha)^2 instead of (1 - alpha) — which is
 * exactly two EMA steps' worth of decay per sample: the tail runs
 * 1.52, 0.55, 0.20, 0.07, ... and is gone in about half the
 * samples. The trade-off is deliberate: the integrated smoothed
 * total now undershoots the true volume slightly, because part of
 * the tail is cut. The raw TrafficStats totals are unaffected.
 * The state is a single Double; there is no window to store.
 */
class SpeedSmoother(windowSamples: Int = DEFAULT_WINDOW) {
    companion object {
        val WINDOW_OPTIONS = intArrayOf(2, 3, 4, 6, 9, 14)
        const val DEFAULT_WINDOW = 4

        fun alphaFor(windowSamples: Int): Double = 2.0 / (windowSamples + 1)
    }

    var windowSamples: Int = windowSamples
        set(value) {
            require(value in WINDOW_OPTIONS) { "unsupported window: $value" }
            field = value
        }

    private var value: Double? = null

    fun update(rawBytesPerSecond: Double): Double {
        val alpha = alphaFor(windowSamples)
        val prev = value
        val next = if (prev == null) {
            rawBytesPerSecond
        } else if (rawBytesPerSecond >= prev) {
            alpha * rawBytesPerSecond + (1.0 - alpha) * prev
        } else {
            // Falling: square the retention for a faster decay.
            val retention = (1.0 - alpha) * (1.0 - alpha)
            (1.0 - retention) * rawBytesPerSecond + retention * prev
        }
        value = next
        return next
    }

    fun reset() {
        value = null
    }
}
