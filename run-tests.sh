#!/bin/bash
# Runs the plain-JVM unit tests (no Gradle, no Android runtime).
set -e
cd "$(dirname "$0")"
export JAVA_HOME="$HOME/workspace/toolchains/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
KOTLINC="$HOME/workspace/toolchains/kotlinc/bin/kotlinc"
KOTLIN_STDLIB="$HOME/workspace/toolchains/kotlinc/lib/kotlin-stdlib.jar"
OUT="$PWD/build-tests"
rm -rf "$OUT"; mkdir -p "$OUT"
"$KOTLINC" -d "$OUT/tests.jar" \
  app/src/main/java/dev/moriya/speedmeterlab/SpeedFormat.kt \
  tests/SpeedFormatTest.kt
java -cp "$OUT/tests.jar:$KOTLIN_STDLIB" dev.moriya.speedmeterlab.SpeedFormatTestKt
