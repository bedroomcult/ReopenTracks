#!/bin/bash
# Build + sign the ReopenTracks release APK.
#
# Run this on the Windows box that holds JDK 17, the Android SDK (API 36 +
# build-tools 36.0.0) and the release keystore. See docs/BUILD_AND_RELEASE.md
# sections 1, 2, 4 and 6.
#
# The keystore password is NEVER stored in this repo. Export it for this shell
# only, then run the script:
#
#   export RELEASE_STORE_PASSWORD='...'
#   export RELEASE_KEY_PASSWORD='...'
#   bash RELEASE_SIGN.sh
#
# Defaults match BUILD_AND_RELEASE.md. Override via env if your paths differ.

set -euo pipefail

# ---------------------------------------------------------------- config ----
: "${ANDROID_HOME:=C:/Users/Graphic/AppData/Local/Android/Sdk}"
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"

: "${RELEASE_STORE_FILE:=C:/Users/Graphic/.reopentracks-keys/reopen-tracks-release.jks}"
: "${RELEASE_KEY_ALIAS:=reopentracks}"
: "${RELEASE_STORE_PASSWORD:?set RELEASE_STORE_PASSWORD (never commit it)}"
: "${RELEASE_KEY_PASSWORD:?set RELEASE_KEY_PASSWORD}"

BT="$ANDROID_HOME/build-tools/36.0.0"
REL="build/outputs/apk/irreproducible/release"

# ------------------------------------------------------------------ preflight
echo "==> Preflight"
[ -f "$RELEASE_STORE_FILE" ] || { echo "keystore not found: $RELEASE_STORE_FILE"; exit 1; }
[ -d "$BT" ] || { echo "build-tools not found: $BT"; exit 1; }
command -v java >/dev/null || { echo "java not on PATH (need JDK 17)"; exit 1; }

# versionCode/versionName come from build.gradle, set by the release commit.
VERSION_CODE=$(sed -n 's/^\s*versionCode\s\+\([0-9]\+\).*/\1/p' build.gradle)
VERSION_NAME=$(sed -n 's/^\s*versionName\s\+"\(v[^"]*\)".*/\1/p' build.gradle)
[ -n "$VERSION_CODE" ] && [ -n "$VERSION_NAME" ] || { echo "cannot read version from build.gradle"; exit 1; }
echo "    versionCode=$VERSION_CODE versionName=$VERSION_NAME"

# Sanity: versionCode must equal the commit count, per docs/VERSIONING.md.
EXPECTED_CODE=$(git rev-list HEAD --count main)
if [ "$VERSION_CODE" != "$EXPECTED_CODE" ]; then
  echo "    WARNING: versionCode $VERSION_CODE != git commit count $EXPECTED_CODE"
fi

# -------------------------------------------------------------------- build --
# build.gradle reads signing material from -D system properties, but we sign
# manually below, so build unsigned and keep the keystore out of the build log.
echo "==> Building unsigned release APK"
./gradlew assembleIrreproducibleRelease --stacktrace

UNSIGNED="$REL/com.reopentracks_${VERSION_CODE}.apk"
[ -f "$UNSIGNED" ] || { echo "expected APK not found: $UNSIGNED"; ls -la "$REL" 2>/dev/null; exit 1; }

# --------------------------------------------------------------- align+sign -
echo "==> Aligning"
"$BT/zipalign.exe" -f -p 4 "$UNSIGNED" "$REL/aligned.apk"

echo "==> Signing"
OUT="$REL/ReopenTracks-${VERSION_NAME}-release.apk"
"$BT/apksigner.bat" sign \
  --ks "$RELEASE_STORE_FILE" \
  --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --ks-pass "env:RELEASE_STORE_PASSWORD" \
  --key-pass "env:RELEASE_KEY_PASSWORD" \
  --out "$OUT" "$REL/aligned.apk"

# ------------------------------------------------------------------- verify -
echo "==> Verifying signature"
"$BT/apksigner.bat" verify --print-certs "$OUT"

# Expected cert SHA-256 (docs/BUILD_AND_RELEASE.md section 4):
#   54ca5f898a4cf712ae8dfdfe2cad3faf2d5ff8a7a96a226df99d2ec7d532596d
CERT=$( "$BT/apksigner.bat" verify --print-certs "$OUT" 2>/dev/null \
        | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | tr 'A-Z' 'a-z' )
EXPECTED_CERT="54ca5f898a4cf712ae8dfdfe2cad3faf2d5ff8a7a96a226df99d2ec7d532596d"
if [ "$CERT" != "$EXPECTED_CERT" ]; then
  echo "    WARNING: certificate SHA-256 mismatch"
  echo "      got:      $CERT"
  echo "      expected: $EXPECTED_CERT"
else
  echo "    certificate OK"
fi

echo "==> Badging"
"$BT/aapt2.exe" dump badging "$OUT" | grep -E "^package:" || true

echo
echo "Done: $OUT"
echo "Next: install on a device, export a track as KMZ, and confirm it renders in Google Earth/OsmAnd."
echo "Then create the GitHub release and upload this APK (docs/BUILD_AND_RELEASE.md section 5)."
