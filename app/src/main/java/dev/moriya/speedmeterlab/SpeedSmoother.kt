package dev.moriya.speedmeterlab

/**
 * Exponential moving average over the raw bytes-per-second samples.
 *
 * The strength is expressed as an effective window length N in
 * samples, converted with the standard relation alpha = 2 / (N + 1).
 * The settled default N = 4 gives alpha 0.4: the sequence 0, 3.8, 0
 * (MB/s) displays roughly as 1.52, 0.91, 0.55, 0.33, ... — a spike is
 * kept, but decays geometrically instead of vanishing in one step.
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
        } else {
            alpha * rawBytesPerSecond + (1.0 - alpha) * prev
        }
        value = next
        return next
    }

    fun reset() {
        value = null
    }
}
