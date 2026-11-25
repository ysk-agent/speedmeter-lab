package dev.moriya.speedmeterlab

import kotlin.math.abs

/** Plain-JVM assertions for SpeedFormat and SpeedSmoother; run by run-tests.sh. */
fun main() {
    var n = 0
    fun check(input: Double, expected: String) {
        val actual = SpeedFormat.format(input)
        check(actual == expected) { "format($input) = $actual, expected $expected" }
        n++
    }
    check(0.0, "0.00 MB/s")
    check(-5.0, "0.00 MB/s")
    check(Double.NaN, "0.00 MB/s")
    check(4_999.0, "0.00 MB/s")       // below 0.005 MB/s rounds to 0.00
    check(300_000.0, "0.30 MB/s")     // 300 KB/s
    check(1_000_000.0, "1.00 MB/s")
    check(1_140_000.0, "1.14 MB/s")
    check(1_236_000.0, "1.24 MB/s")
    check(12_340_000.0, "12.34 MB/s")
    check(99_990_000.0, "99.99 MB/s")

    // EMA: the 0, 3.8, 0 spike (in bytes/s) decays geometrically.
    val smoother = SpeedSmoother()
    fun checkEma(rawBps: Double, expectedBps: Double) {
        val actual = smoother.update(rawBps)
        check(abs(actual - expectedBps) < 1.0) { "ema($rawBps) = $actual, expected $expectedBps" }
        n++
    }
    checkEma(0.0, 0.0)
    checkEma(3_800_000.0, 1_520_000.0)
    checkEma(0.0, 912_000.0)
    checkEma(0.0, 547_200.0)
    smoother.reset()
    checkEma(2_000_000.0, 2_000_000.0)  // after reset, first sample passes through

    println("SpeedFormatTest: $n assertions passed")
}
