package dev.moriya.speedmeterlab

/** Plain-JVM assertions for SpeedFormat; run by run-tests.sh. */
fun main() {
    var n = 0
    fun check(input: Double, expected: String) {
        val actual = SpeedFormat.format(input)
        check(actual == expected) { "format($input) = $actual, expected $expected" }
        n++
    }
    check(0.0, "0.0 MB/s")
    check(-5.0, "0.0 MB/s")
    check(Double.NaN, "0.0 MB/s")
    check(49_999.0, "0.0 MB/s")       // below 0.05 MB/s rounds to 0.0
    check(300_000.0, "0.3 MB/s")      // the spec example: 300 KB/s
    check(1_000_000.0, "1.0 MB/s")
    check(1_240_000.0, "1.2 MB/s")
    check(1_260_000.0, "1.3 MB/s")
    check(12_300_000.0, "12.3 MB/s")
    check(99_940_000.0, "99.9 MB/s")
    println("SpeedFormatTest: $n assertions passed")
}
