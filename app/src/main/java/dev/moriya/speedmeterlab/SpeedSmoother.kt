package dev.moriya.speedmeterlab

/**
 * Exponential moving average over the raw bytes-per-second samples.
 *
 * A single-sample spike is kept but decays geometrically instead of
 * vanishing in one step: with ALPHA = 0.4, the sequence 0, 3.8, 0
 * (MB/s) displays roughly as 1.52, 0.91, 0.55, 0.33, ... The state is
 * a single Double; there is no window to store.
 */
class SpeedSmoother {
    companion object {
        const val ALPHA = 0.4
    }

    private var value: Double? = null

    fun update(rawBytesPerSecond: Double): Double {
        val prev = value
        val next = if (prev == null) {
            rawBytesPerSecond
        } else {
            ALPHA * rawBytesPerSecond + (1.0 - ALPHA) * prev
        }
        value = next
        return next
    }

    fun reset() {
        value = null
    }
}
