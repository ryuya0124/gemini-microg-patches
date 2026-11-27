#!/usr/bin/env bash
# Experimental diagnostic bundle on top of the stable patches. Never touches the stable feed.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
out="$BUILD_DIR/diagnostics"
android_jar="${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}"
require_file "$android_jar"
require_file "$ROOT/tools/r8.jar"
rm -rf "$out/release" "$out/resources"
mkdir -p "$out/tests" "$out/resources/extensions" "$out/resources/diagnostics"
javac -encoding UTF-8 -d "$out/tests" "$ROOT/diagnostics/runtime/app/geminipatch/diagnostics/CaptureInbox.java" "$ROOT/diagnostics/tests/CaptureInboxTest.java"
"$JAVA" -cp "$out/tests" app.geminipatch.diagnostics.CaptureInboxTest
node "$ROOT/diagnostics/package.mjs" schema "$out"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android_jar" -d "$out/runtime/classes" \
  "$ROOT/diagnostics/runtime/app/geminipatch/diagnostics/"*.java "$out/runtime/Schema.java"
jar cf "$out/runtime/runtime.jar" -C "$out/runtime/classes" .
"$JAVA" -cp "$ROOT/tools/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$android_jar" \
  --output "$out/runtime/dex" "$out/runtime/runtime.jar"
cp "$out/runtime/dex/classes.dex" "$out/resources/extensions/conversation-diagnostics.bin"
"$ROOT/scripts/compile-patches.sh"
"$KOTLINC" "$ROOT/diagnostics/patches" -cp "$MORPHE:$BUILD_DIR/gemini-patches.jar" -module-name geminiDiagnostics -d "$out/addon.jar"
node "$ROOT/diagnostics/package.mjs" merge "$out"
"$JAVA" -cp "$ROOT/tools/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$android_jar" --classpath "$MORPHE" \
  --output "$out/patch-dex" "$out/diagnostics-patches.jar"
node "$ROOT/diagnostics/package.mjs" package "$out"
version="$(jq -er '.version' "$ROOT/diagnostics/metadata.json")"
bundle="$out/release/gemini-microg-patches-$version.mpp"
"$KOTLINC" "$ROOT/scripts/kotlin/VerifyMpp.kt" "$ROOT/diagnostics/VerifyDiagnostics.kt" -cp "$MORPHE:$out/diagnostics-patches.jar" -d "$out/verify.jar"
"$JAVA" -cp "$out/verify.jar:$MORPHE" VerifyMppKt "$bundle" "$version" diagnostics
"$JAVA" -cp "$out/verify.jar:$MORPHE:$out/diagnostics-patches.jar" VerifyDiagnosticsKt "$bundle" "$(node "$ROOT/diagnostics/package.mjs" token "$out")"
"$JAVA" -jar "$MORPHE" list-patches --patches "$bundle" --out "$out/patch-list.txt" >/dev/null
printf 'Experimental bundle: %s\n' "$bundle"
