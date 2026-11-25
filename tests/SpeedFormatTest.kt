package dev.moriya.speedmeterlab

import kotlin.math.abs

/** Plain-JVM assertions for SpeedFormat and SpeedSmoother; run by run-tests.sh. */
fun main() {
    var n = 0
    fun check(input: Double, decimals: Int, expected: String) {
        val actual = SpeedFormat.format(input, decimals)
        check(actual == expected) { "format($input, $decimals) = $actual, expected $expected" }
        n++
    }
    // Two decimals (the default).
    check(0.0, 2, "0.00 MB/s")
    check(-5.0, 2, "0.00 MB/s")
    check(Double.NaN, 2, "0.00 MB/s")
    check(4_999.0, 2, "0.00 MB/s")
    check(300_000.0, 2, "0.30 MB/s")
    check(1_140_000.0, 2, "1.14 MB/s")
    check(12_340_000.0, 2, "12.34 MB/s")
    // One decimal.
    check(0.0, 1, "0.0 MB/s")
    check(300_000.0, 1, "0.3 MB/s")
    check(1_260_000.0, 1, "1.3 MB/s")
    // Zero decimals.
    check(0.0, 0, "0 MB/s")
    check(1_400_000.0, 0, "1 MB/s")
    check(1_600_000.0, 0, "2 MB/s")

    // EMA with the default window N=4 (alpha 0.4): 0, 3.8, 0 decays geometrically.
    val smoother = SpeedSmoother()
    fun checkEma(s: SpeedSmoother, rawBps: Double, expectedBps: Double) {
        val actual = s.update(rawBps)
        check(abs(actual - expectedBps) < 1.0) { "ema($rawBps) = $actual, expected $expectedBps" }
        n++
    }
    checkEma(smoother, 0.0, 0.0)
    checkEma(smoother, 3_800_000.0, 1_520_000.0)
    checkEma(smoother, 0.0, 912_000.0)
    checkEma(smoother, 0.0, 547_200.0)
    smoother.reset()
    checkEma(smoother, 2_000_000.0, 2_000_000.0)  // after reset, first sample passes through

    // Window N=9 (alpha 0.2): the same spike peaks lower and decays slower.
    val slow = SpeedSmoother(9)
    checkEma(slow, 0.0, 0.0)
    checkEma(slow, 3_800_000.0, 760_000.0)
    checkEma(slow, 0.0, 608_000.0)

    println("SpeedFormatTest: $n assertions passed")
}
