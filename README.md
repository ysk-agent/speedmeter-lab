# Speed Meter Lab

A standalone, verification-only Android app for tuning the sampling
interval of the low-power network speed meter that will ship in
Moka VPN. It exists so the interval can be compared on a real device
without reinstalling Moka VPN for every candidate value.

- Package: `dev.moriya.speedmeterlab` (separate from Moka VPN's
  `dev.moriya.vpn`), signed with its own dedicated key.
- Polls `TrafficStats.getTotalRxBytes()` / `getTotalTxBytes()` and
  shows the current speed in the status bar. No recording, no logs.
- Display format is fixed to "XX.X MB/s" (1 MB = 1,000,000 bytes,
  0.1 MB/s steps; 300 KB/s shows as "0.3 MB/s"). The notification icon
  is a bitmap rendered from the string and is updated only when the
  string changes.
- Sampling interval is selectable at runtime from a dropdown:
  1 / 2 / 3 / 5 / 10 seconds, default 1 second.
- Polling stops while the screen is off and while the meter switch
  is off.

## Build

No Gradle: this VM cannot run Gradle (JVM loopback TCP is
intercepted), and this app is framework-only, so the manual pipeline
in `build-manual.sh` (aapt2 → kotlinc → d8 → zipalign → apksigner)
builds it locally in seconds. The signing keystore lives outside the
repository at `~/workspace/speedmeter-lab-signing/`.

Unit tests for the pure formatter: `./run-tests.sh`.
