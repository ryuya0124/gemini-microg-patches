#!/usr/bin/env bash
# Private emulator instrumentation only. Never include the resulting APK in a release.
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
require_file "${KEYSTORE_PATH:?Set KEYSTORE_PATH to the existing target APK signing key}"
: "${GEMINIPATCH_STORE_PASS:?Set GEMINIPATCH_STORE_PASS privately}"
out="$BUILD_DIR/diagnostic-probes"
mkdir -p "$out/classes" "$out/dex"
android="$ANDROID_HOME/platforms/android-36/android.jar"; bt="$ANDROID_HOME/build-tools/36.0.0"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android" -d "$out/classes" \
 "$ROOT/diagnostics/tests/NativeCaptureProbe.java" "$ROOT/diagnostics/tests/BuildIdentityProbe.java" \
 "$ROOT/diagnostics/experiments/history-update-compat/tests/UpdateCompatibilityProbe.java"
jar cf "$out/probes.jar" -C "$out/classes" .
"$JAVA" -cp "$ROOT/tools/r8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --output "$out/dex" "$out/probes.jar"
"$bt/aapt2" link -I "$android" --manifest "$ROOT/diagnostics/tests/AndroidManifest.xml" -o "$out/unsigned.apk"
python3 - "$out" <<'PY'
from pathlib import Path
import sys, zipfile
out = Path(sys.argv[1])
with zipfile.ZipFile(out/'unsigned.apk', 'a') as archive:
    archive.write(out/'dex/classes.dex', 'classes.dex')
PY
export GEMINIPATCH_STORE_PASS
"$bt/apksigner" sign --ks "$KEYSTORE_PATH" --ks-key-alias "${KEY_ALIAS:-geminipatch}" --ks-pass env:GEMINIPATCH_STORE_PASS --key-pass env:GEMINIPATCH_STORE_PASS --out "$out/probe.apk" "$out/unsigned.apk"
"$bt/apksigner" verify "$out/probe.apk"
printf 'Private emulator probe: %s\n' "$out/probe.apk"
