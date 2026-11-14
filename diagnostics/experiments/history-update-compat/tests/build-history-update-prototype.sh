#!/usr/bin/env bash
set -euo pipefail
cd /Users/ryuya/Documents/GeminiPatch
source scripts/lib/common.sh
out="$ROOT/local/issue8/reporter-diagnostics3/update-compat"
mkdir -p "$out/classes" "$out/dex"
android="$ANDROID_HOME/platforms/android-36/android.jar"
javac -source 8 -target 8 -bootclasspath "$android" -d "$out/classes" "$out/HistoryUpdateCompatibility.java" "$out/HistoryUpdateFixture.java"
jar cf "$out/runtime.jar" -C "$out/classes" .
"$JAVA" -cp tools/r8.jar com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --output "$out/dex" "$out/runtime.jar"
cp "$out/dex/classes.dex" "$out/update.bin"
"$KOTLINC" "$out/HistoryUpdatePatch.kt" -cp "$MORPHE" -d "$out/prototype.jar"
jar uf "$out/prototype.jar" -C "$out" update.bin
scripts/patch-apk.sh /Users/ryuya/Documents/GeminiPatch-diagnostics/local/verification/d3-build/google.apk "$out/google-unsigned.apk" "$out/result.json" -p "$out/prototype.jar" >"$out/patch.log" 2>&1
zip -dq "$out/google-unsigned.apk" META-INF/GEMINIPA.SF META-INF/GEMINIPA.RSA META-INF/MANIFEST.MF
export GEMINIPATCH_STORE_PASS
key="${KEYSTORE_PATH:-local/keys/gemini-release.p12}";[[ "$key" = /* ]] || key="$ROOT/$key"
bt="$ANDROID_HOME/build-tools/36.0.0"
"$bt/zipalign" -P 16 -f 4 "$out/google-unsigned.apk" "$out/google-aligned.apk"
"$bt/apksigner" sign --alignment-preserved true --ks "$key" --ks-key-alias "${KEY_ALIAS:-geminipatch}" --ks-pass env:GEMINIPATCH_STORE_PASS --key-pass env:GEMINIPATCH_STORE_PASS --out "$out/google.apk" "$out/google-aligned.apk"
"$bt/apksigner" verify "$out/google.apk";"$bt/zipalign" -c -P 16 4 "$out/google.apk"
echo 'Built private update-warning prototype and synthetic fixture; not for distribution'
