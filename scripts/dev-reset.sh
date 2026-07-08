#!/usr/bin/env bash
# Full teardown + rebuild + relaunch cycle for local emulator testing.
#
# - Kills any running emulator
# - Rebuilds the debug APK from a clean state
# - Boots the Pixel_10 AVD
# - Uninstalls any previous install (avoids stale-Room-schema crashes)
# - Installs the freshly built APK
# - Pushes everything in ./books to /sdcard/Books on the device
# - Launches the app
#
# Note: a clean uninstall wipes the granted Books-folder SAF permission, so
# you'll need to re-grant /sdcard/Books via the in-app folder picker once
# after this script finishes.

set -euo pipefail

AVD_NAME="Pixel_10"
PACKAGE="com.tehuti.reader"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BOOKS_DIR="$REPO_ROOT/books"

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

echo "==> Tearing down: killing any running emulator"
adb emu kill >/dev/null 2>&1 || true
# Give the emulator process a moment to actually exit before we relaunch.
for _ in $(seq 1 15); do
  adb devices | grep -q "emulator-" || break
  sleep 1
done

echo "==> Rebuilding (clean + assembleDebug)"
cd "$REPO_ROOT"
./gradlew clean :app:assembleDebug -q

echo "==> Starting emulator: $AVD_NAME"
nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" > /tmp/tehuti-emulator.log 2>&1 &
disown

echo "==> Waiting for device"
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

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
