#!/usr/bin/env bash
# Rebuild + reinstall + relaunch cycle targeting a physical Pixel 7 paired
# over Wireless debugging (the device-flavored counterpart to dev-reset.sh).
#
# - Finds the Pixel 7 among connected adb devices (no hardcoded serial —
#   wireless-debugging serials change between pairings)
# - Rebuilds the debug APK from a clean state
# - Uninstalls any previous install (avoids stale-Room-schema crashes)
# - Installs the freshly built APK
# - Pushes everything in ./books to /sdcard/Books on the device
# - Launches the app
#
# The phone must already be paired and connected via Wireless debugging —
# this script won't pair for you. If it's not listed in `adb devices`,
# reconnect from the phone's Developer options first.
#
# Note: a clean uninstall wipes the granted Books-folder SAF permission, so
# you'll need to re-grant /sdcard/Books via the in-app folder picker once
# after this script finishes.

set -euo pipefail

DEVICE_MODEL="Pixel_7"
PACKAGE="com.tehuti.reader"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BOOKS_DIR="$REPO_ROOT/books"

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

echo "==> Looking for a connected $DEVICE_MODEL"
SERIAL="$(adb devices -l | awk -v model="model:$DEVICE_MODEL" \
  '$2 == "device" && index($0, model) { print $1; exit }')"
if [ -z "$SERIAL" ]; then
  echo "error: no $DEVICE_MODEL found in 'adb devices'." >&2
  echo "Connect it via Wireless debugging (Developer options) and retry." >&2
  adb devices -l >&2
  exit 1
fi
echo "    Found: $SERIAL"

# Everything below (adb and Gradle's installDebug alike) targets this device,
# even if an emulator is also running.
export ANDROID_SERIAL="$SERIAL"

echo "==> Rebuilding (clean + assembleDebug)"
cd "$REPO_ROOT"
./gradlew clean :app:assembleDebug -q

echo "==> Uninstalling any previous build (clean slate for Room)"
adb uninstall "$PACKAGE" >/dev/null 2>&1 || true

echo "==> Installing freshly built APK"
./gradlew :app:installDebug -q

echo "==> Pushing books from $BOOKS_DIR to /sdcard/Books"
adb shell mkdir -p /sdcard/Books
if [ -d "$BOOKS_DIR" ] && [ -n "$(ls -A "$BOOKS_DIR" 2>/dev/null)" ]; then
  adb push "$BOOKS_DIR"/. /sdcard/Books/
else
  echo "    (no files found in $BOOKS_DIR, skipping)"
fi

echo "==> Launching tehuti"
adb shell am start -W -n "$PACKAGE/.MainActivity"

echo "==> Done. Books were pushed to /sdcard/Books — since this was a fresh"
echo "    install, grant that folder via the in-app folder picker."
