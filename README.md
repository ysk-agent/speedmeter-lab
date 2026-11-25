# Speed Meter Lab

A standalone, verification-only Android app for tuning the sampling
interval of the low-power network speed meter that will ship in
Moka VPN. It exists so the interval can be compared on a real device
without reinstalling Moka VPN for every candidate value.

- Package: `dev.moriya.speedmeterlab` (separate from Moka VPN's
  `dev.moriya.vpn`), signed with its own dedicated key.
- Polls `TrafficStats.getTotalRxBytes()` / `getTotalTxBytes()` and
  shows the current speed in the status bar. No recording, no logs.
- Display is always in MB/s (1 MB = 1,000,000 bytes); the number of
  decimals is selectable (0 / 1 / 2, default 2, e.g. "1.14 MB/s").
  The notification icon is a bitmap showing the number only (no
  unit), updated only when the string changes. The icon's padding
  is selectable (0 / 5 / 10 / 15 / 20 / 25 percent of the icon size,
  default 5), and a padding change re-renders the icon even when
  the value is static. Sizing and centring use the ink bounds
  (Paint.getTextBounds), so the padding setting is the visual
  padding — the font metrics' own reserves add nothing on top.
- Sampling interval is selectable at runtime from a dropdown:
  1 / 2 / 3 / 5 / 10 seconds, default 1 second.
- An optional smoothing switch applies an exponential moving average
  to the displayed value, so single-sample spikes decay geometrically
  instead of vanishing in one step. Off by default. Its strength is
  an effective window in samples (2 / 3 / 4 / 6 / 9 / 14, default 4,
  converted with alpha = 2 / (N + 1)).
- Polling stops while the screen is off and while the meter switch
  is off.

## Build

No Gradle: this VM cannot run Gradle (JVM loopback TCP is
intercepted), and this app is framework-only, so the manual pipeline
in `build-manual.sh` (aapt2 → kotlinc → d8 → zipalign → apksigner)
builds it locally in seconds. The signing keystore lives outside the
repository at `~/workspace/speedmeter-lab-signing/`.

Unit tests for the pure formatter: `./run-tests.sh`.
