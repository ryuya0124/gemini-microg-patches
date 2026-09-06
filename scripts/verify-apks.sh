#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib/common.sh"
for name in VerifyDexBranches VerifyWorkProfile; do
  "$KOTLINC" "$ROOT/scripts/kotlin/$name.kt" -cp "$MORPHE" -d "$BUILD_DIR/$name.jar"
done
"$JAVA" -cp "$BUILD_DIR/VerifyDexBranches.jar:$MORPHE" VerifyDexBranchesKt "$BUILD_DIR/google-login-unsigned.apk"
"$JAVA" -cp "$BUILD_DIR/VerifyWorkProfile.jar:$MORPHE" VerifyWorkProfileKt "$ROOT/inputs/google-base.apk" "$BUILD_DIR/google-login-unsigned.apk"
