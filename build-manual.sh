#!/bin/bash
# Manual APK build pipeline for speedmeter-lab (no Gradle).
#
# Why: Gradle cannot run on this VM (JVM loopback TCP is intercepted,
# so the Gradle client <-> daemon IPC fails). This app is deliberately
# framework-only (no AndroidX, no external dependencies), so the
# manual pipeline covers it completely:
#   aapt2 compile+link -> kotlinc -> d8 -> zip -> zipalign -> apksigner
# Toolchain pieces live under ~/workspace/toolchains/ (fetched with
# curl), the same arrangement as ~/workspace/phone-node/build-manual.sh
# and ~/workspace/moka-vpn/build-manual.sh.
#
# Signing: a dedicated keystore for this app only, kept OUTSIDE the
# repository at ~/workspace/speedmeter-lab-signing/ (keystore +
# credentials.txt with "store_pass: / key_pass: / key_alias:" lines).
#
# Output: dist/speedmeter-lab.apk + dist/update.json
set -e
cd "$(dirname "$0")"

VERSION_CODE=13
VERSION_NAME=0.12.0

export JAVA_HOME="$HOME/workspace/toolchains/jdk-21"
export ANDROID_HOME="$HOME/workspace/toolchains/android-sdk"
export PATH="$JAVA_HOME/bin:$PATH"

BT="$ANDROID_HOME/build-tools/35.0.0"
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
KOTLIN_STDLIB="$HOME/workspace/toolchains/kotlinc/lib/kotlin-stdlib.jar"
KOTLINC="$HOME/workspace/toolchains/kotlinc/bin/kotlinc"
SIGNING_DIR="$HOME/workspace/speedmeter-lab-signing"
KEYSTORE="$SIGNING_DIR/speedmeter-lab.keystore"
OUT="$PWD/build-manual"
DIST="$PWD/dist"

[ -f "$KEYSTORE" ] || { echo "missing $KEYSTORE" >&2; exit 1; }

# Parse (never source) the signing credentials file.
STORE_PASS=$(sed -n 's/^store_pass: //p' "$SIGNING_DIR/credentials.txt")
KEY_PASS=$(sed -n 's/^key_pass: //p' "$SIGNING_DIR/credentials.txt")
KEY_ALIAS=$(sed -n 's/^key_alias: //p' "$SIGNING_DIR/credentials.txt")
[ -n "$STORE_PASS" ] && [ -n "$KEY_PASS" ] && [ -n "$KEY_ALIAS" ] \
  || { echo "could not parse $SIGNING_DIR/credentials.txt" >&2; exit 1; }

echo "=== preparing ==="
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/dex" "$DIST"

echo "=== aapt2 compile + link ==="
"$BT/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest app/src/main/AndroidManifest.xml \
  --min-sdk-version 29 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  --java "$OUT/gen" \
  "$OUT/res.zip"

echo "=== kotlinc ==="
SOURCES=$(find app/src/main/java -name '*.kt'; find "$OUT/gen" -name '*.java')
# shellcheck disable=SC2086
"$KOTLINC" -jvm-target 17 \
  -classpath "$ANDROID_JAR" \
  -d "$OUT/classes.jar" $SOURCES

echo "=== d8 ==="
"$BT/d8" --lib "$ANDROID_JAR" --min-api 29 \
  --output "$OUT/dex" "$OUT/classes.jar" "$KOTLIN_STDLIB"

echo "=== package ==="
cp "$OUT/base.apk" "$OUT/speedmeter-lab.apk"
(cd "$OUT/dex" && zip -q "../speedmeter-lab.apk" classes.dex)

"$BT/zipalign" -p -f 4 "$OUT/speedmeter-lab.apk" "$OUT/speedmeter-lab-aligned.apk"
"$BT/apksigner" sign --ks "$KEYSTORE" \
  --ks-pass "pass:$STORE_PASS" --key-pass "pass:$KEY_PASS" \
  --ks-key-alias "$KEY_ALIAS" \
  --out "$DIST/speedmeter-lab.apk" "$OUT/speedmeter-lab-aligned.apk"
"$BT/apksigner" verify --print-certs "$DIST/speedmeter-lab.apk"

SHA256=$(sha256sum "$DIST/speedmeter-lab.apk" | cut -d' ' -f1)
SIZE=$(stat -c%s "$DIST/speedmeter-lab.apk")
printf '{"file":"speedmeter-lab.apk","sha256":"%s","size":%s,"versionCode":%s,"versionName":"%s"}\n' \
  "$SHA256" "$SIZE" "$VERSION_CODE" "$VERSION_NAME" > "$DIST/update.json"
echo "OK: $DIST/speedmeter-lab.apk"
echo "SHA-256: $SHA256"
cat "$DIST/update.json"
