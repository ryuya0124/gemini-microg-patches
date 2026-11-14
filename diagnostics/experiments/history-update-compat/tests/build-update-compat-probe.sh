#!/usr/bin/env bash
set -euo pipefail
cd /Users/ryuya/Documents/GeminiPatch
source scripts/lib/common.sh
out="$ROOT/local/issue8/reporter-diagnostics3/update-compat/probe"
mkdir -p "$out/classes" "$out/dex"
android="$ANDROID_HOME/platforms/android-36/android.jar";bt="$ANDROID_HOME/build-tools/36.0.0"
javac -source 8 -target 8 -bootclasspath "$android" -d "$out/classes" "$out/UpdateCompatibilityProbe.java"
jar cf "$out/probe.jar" -C "$out/classes" .
"$JAVA" -cp tools/r8.jar com.android.tools.r8.D8 --release --min-api 26 --lib "$android" --output "$out/dex" "$out/probe.jar"
"$bt/aapt2" link -I "$android" --manifest "$out/update-probe-AndroidManifest.xml" -o "$out/unsigned.apk"
python3 - "$out" <<'PY'
from pathlib import Path
import sys,zipfile
p=Path(sys.argv[1])
with zipfile.ZipFile(p/'unsigned.apk','a')as z:z.write(p/'dex/classes.dex','classes.dex')
PY
export GEMINIPATCH_STORE_PASS
key="${KEYSTORE_PATH:-local/keys/gemini-release.p12}";[[ "$key" = /* ]] || key="$ROOT/$key"
"$bt/apksigner" sign --ks "$key" --ks-key-alias "${KEY_ALIAS:-geminipatch}" --ks-pass env:GEMINIPATCH_STORE_PASS --key-pass env:GEMINIPATCH_STORE_PASS --out "$out/probe.apk" "$out/unsigned.apk"
"$bt/apksigner" verify "$out/probe.apk"
echo 'Built same-key selective compatibility/native UI probe'
