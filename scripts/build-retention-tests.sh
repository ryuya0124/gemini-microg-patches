#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
[[ $# -ge 1 ]] || fail "Usage: $0 PATCHED_GOOGLE_APK [OUTPUT_DIRECTORY]"
diag="$ROOT"
input="$1"
require_file "$input"
require_file "${KEYSTORE_PATH:?Set KEYSTORE_PATH to the existing signing keystore}"
: "${GEMINIPATCH_STORE_PASS:?Set GEMINIPATCH_STORE_PASS privately}"
out="${2:-$BUILD_DIR/retention-tests}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
mkdir -p "$out/classes" "$out/dex" "$out/probe-classes" "$out/probe-dex" "$out/assets"
android="$ANDROID_HOME/platforms/android-36/android.jar"
bt="$ANDROID_HOME/build-tools/36.0.0"
test="$diag/diagnostics/retention-tests"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android" -cp "$diag/build/diagnostics/retention/classes" -d "$out/classes" "$test/FixtureActivity.java" "$test/BlankActivity.java"
jar cf "$out/runtime.jar" -C "$out/classes" .
"$JAVA" -cp "$ROOT/tools/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --classpath "$diag/build/diagnostics/retention/runtime.jar" --output "$out/dex" "$out/runtime.jar"
cp "$out/dex/classes.dex" "$out/retention-fixture.bin"
"$KOTLINC" "$test/FixturePatch.kt" -cp "$MORPHE" -d "$out/fixture.jar"
jar uf "$out/fixture.jar" -C "$out" retention-fixture.bin
"$diag/scripts/patch-apk.sh" "$input" "$out/google-unsigned.apk" "$out/result.json" -p "$out/fixture.jar" >"$out/patch.log" 2>&1
zip -dq "$out/google-unsigned.apk" META-INF/GEMINIPA.SF META-INF/GEMINIPA.RSA META-INF/MANIFEST.MF
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android" -d "$out/probe-classes" "$test/RetentionProbe.java"
jar cf "$out/probe.jar" -C "$out/probe-classes" .
"$JAVA" -cp "$ROOT/tools/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --output "$out/probe-dex" "$out/probe.jar"
"${FFMPEG:-ffmpeg}" -y -v error -f lavfi -i color=c=green:s=160x90:d=2 -c:v libx264 -pix_fmt yuv420p -movflags +faststart "$out/assets/retention-test.mp4"
"$bt/aapt2" link -I "$android" --manifest "$test/AndroidManifest.xml" -A "$out/assets" -o "$out/probe-unsigned.apk"
(cd "$out/probe-dex" && zip -q "$out/probe-unsigned.apk" classes.dex)
export GEMINIPATCH_STORE_PASS
key="${KEYSTORE_PATH:-local/keys/gemini-release.p12}"; [[ "$key" = /* ]] || key="$ROOT/$key"
for name in google probe; do
 "$bt/zipalign" -P 16 -f 4 "$out/$name-unsigned.apk" "$out/$name-aligned.apk"
 "$bt/apksigner" sign --alignment-preserved true --ks "$key" --ks-key-alias "${KEY_ALIAS:-geminipatch}" --ks-pass env:GEMINIPATCH_STORE_PASS --key-pass env:GEMINIPATCH_STORE_PASS --out "$out/$name.apk" "$out/$name-aligned.apk"
 "$bt/apksigner" verify "$out/$name.apk"
done
echo 'Built private same-key retention fixture and probe; not for distribution'
