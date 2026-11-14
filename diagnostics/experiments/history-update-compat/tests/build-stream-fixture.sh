#!/usr/bin/env bash
set -euo pipefail
cd /Users/ryuya/Documents/GeminiPatch
source scripts/lib/common.sh
diag=/Users/ryuya/Documents/GeminiPatch-diagnostics
out="$diag/local/verification/preview3-stream-fixture"
mkdir -p "$out/classes" "$out/dex"
sources="$diag/diagnostics/experiments/history-update-compat/tests"
android="$ANDROID_HOME/platforms/android-36/android.jar"
javac -source 8 -target 8 -bootclasspath "$android" -cp "$diag/build/diagnostics/runtime/runtime.jar" -d "$out/classes" "$sources/StreamUpdateFixture.java"
jar cf "$out/runtime.jar" -C "$out/classes" .
"$JAVA" -cp tools/r8.jar com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --classpath "$diag/build/diagnostics/runtime/runtime.jar" --output "$out/dex" "$out/runtime.jar"
cp "$out/dex/classes.dex" "$out/stream.bin"
"$KOTLINC" "$sources/StreamFixturePatch.kt" -cp "$MORPHE" -d "$out/prototype.jar"
jar uf "$out/prototype.jar" -C "$out" stream.bin
scripts/patch-apk.sh "$diag/local/verification/preview3-build/google.apk" "$out/unsigned.apk" "$out/result.json" -p "$out/prototype.jar" >"$out/patch.log" 2>&1
zip -dq "$out/unsigned.apk" META-INF/GEMINIPA.SF META-INF/GEMINIPA.RSA META-INF/MANIFEST.MF
export GEMINIPATCH_STORE_PASS
key="${KEYSTORE_PATH:-local/keys/gemini-release.p12}";[[ "$key" = /* ]] || key="$ROOT/$key"
bt="$ANDROID_HOME/build-tools/36.0.0"
"$bt/zipalign" -P 16 -f 4 "$out/unsigned.apk" "$out/aligned.apk"
"$bt/apksigner" sign --alignment-preserved true --ks "$key" --ks-key-alias "${KEY_ALIAS:-geminipatch}" --ks-pass env:GEMINIPATCH_STORE_PASS --key-pass env:GEMINIPATCH_STORE_PASS --out "$out/google.apk" "$out/aligned.apk"
"$bt/apksigner" verify "$out/google.apk"
"$bt/zipalign" -c -P 16 4 "$out/google.apk"
