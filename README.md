# tehuti

A minimalist Android e-reader (EPUB, with PDF planned) built on the
[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit). See
[CLAUDE.md](./CLAUDE.md) for the full design/build plan and phase-by-phase
acceptance criteria.

- **Package:** `com.tehuti.reader`
- **Min SDK:** 26 · **Target/compile SDK:** 37
- **UI:** Jetpack Compose (Material 3), single Activity + Navigation-Compose
- **Stack:** Hilt, Room, DataStore, Retrofit, Coil, Readium 3.3.0

## Prerequisites

- Android Studio (any recent version — used here for its bundled JDK and SDK)
- An Android SDK checkout at `$HOME/Library/Android/sdk` (default location on
  macOS if installed via Android Studio)
- At least one emulator (AVD) created, or a physical device with USB
  debugging enabled

Set these two environment variables before running any command below:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

`JAVA_HOME` must point at Android Studio's bundled JDK — the project uses
AGP's built-in Kotlin support, which is picky about JDK version.

## Building

From the repo root:

```bash
./gradlew :app:assembleDebug   # build only
./gradlew :app:installDebug    # build + install on a running/connected device
```

## Running in the emulator

### List and start an AVD

```bash
"$ANDROID_HOME/emulator/emulator" -list-avds

# Start one (runs in the background, keeps the window open)
"$ANDROID_HOME/emulator/emulator" -avd <avd_name> &
```

If you don't have an AVD yet, create one in Android Studio's Device Manager,
or via `avdmanager`. A software-rendered (`hw.gpu.enabled=no`) image tends to
be the most stable for this project — some GPU-accelerated preview system
images have shown flaky WebView rendering with Readium's EPUB navigator.

Wait for boot before installing:

```bash
"$ANDROID_HOME/platform-tools/adb" wait-for-device
until [ "$("$ANDROID_HOME/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done
```

### Build, install, and launch

```bash
./gradlew :app:installDebug && \
  "$ANDROID_HOME/platform-tools/adb" shell am start -n com.tehuti.reader/.MainActivity
```

### Clean reinstall (recommended when the schema/version has changed)

`installDebug` reinstalls **over** existing app data. If Room's entity
schema changed since your last install without a matching version bump (or
you're picking up someone else's build), the app can crash on launch with:

```
IllegalStateException: Room cannot verify the data integrity...
```

Avoid this by uninstalling first so no stale database is left behind:

```bash
"$ANDROID_HOME/platform-tools/adb" uninstall com.tehuti.reader 2>/dev/null
./gradlew :app:installDebug && \
  "$ANDROID_HOME/platform-tools/adb" shell am start -n com.tehuti.reader/.MainActivity
```

Note a clean uninstall also wipes the granted Books-folder permission and
all reading progress, so you'll need to re-grant the folder afterward (see
below).

## Loading books

tehuti has no hardcoded books directory — on first launch (or via
**Settings → Books folder → Change**) it opens the system folder picker
(Storage Access Framework) and remembers whatever folder you grant it,
across restarts.

To get real `.epub` files onto the emulator for testing:

```bash
"$ANDROID_HOME/platform-tools/adb" shell mkdir -p /sdcard/Books
"$ANDROID_HOME/platform-tools/adb" push "/path/to/book.epub" /sdcard/Books/
```

Then grant `/sdcard/Books` via the in-app folder picker (or, if it's already
granted, **pull-to-refresh** the library grid to pick up newly pushed
files — the scan only runs on grant or manual refresh, not on a live
filesystem watch).

Any valid, unencrypted EPUB works — DRM-protected books won't open.
[Project Gutenberg](https://www.gutenberg.org) is a good source of free test
files. A deliberately corrupt/garbage `.epub` is also useful for testing the
"couldn't open this book" error path.

## Troubleshooting

- **Blank/white reader screen, or emulator won't render the WebView:** try a
  different AVD/system image — some preview images have shown GPU-related
  WebView issues. A software-rendered image is the safest fallback.
- **`adb: no devices/emulators found`:** no emulator is running. Start one
  (see above) or check `adb devices`.
- **App crashes on launch with a Room identity-hash error:** stale database
  from a previous install — do a clean uninstall/reinstall (see above).
