# tehuti

A minimalist Android e-book reader. No clutter, no accounts, no store — point
it at a folder of EPUB files on your device and read.

- **Distraction-free reading.** Full-screen, immersive view — no toolbars or
  page numbers while you read.
- **Tap-zone navigation.** Tap the left edge of the screen for the previous
  page, the right edge for the next, the middle to bring up controls.
- **Picks up where you left off.** Every book reopens at your last position,
  automatically.
- **Customizable.** Font size, font family, and Light/Dark/Sepia themes.
- **Word lookup.** Long-press any word for a dictionary definition or a
  Wikipedia summary.
- **Spoiler-safe AI reading companion.** Long-press a name or term for an
  AI-generated explanation, or tap the summary icon for a recap of the story
  so far — both are built only from the text you've actually read, never
  from what's still ahead of you.

## Using tehuti

### Adding books

tehuti doesn't come with any books, and it doesn't have a fixed "Books"
folder built in — you choose the folder yourself the first time you open the
app (or later via **Settings → Books folder → Change**), using Android's
standard folder picker. tehuti remembers that folder across restarts.

To add books, just put `.epub` files into that folder using whatever you'd
normally use to move files onto your phone — a file manager app, plugging
the phone into a computer, cloud sync, etc. Then either pull down to refresh
the library grid, or close and reopen tehuti — it rescans the folder and
picks up anything new (and drops anything you removed).

Books must be unencrypted, DRM-free EPUB files. [Project
Gutenberg](https://www.gutenberg.org) is a good source of free ones if you
want something to test with.

### Reading

- **Tap the left ~25% of the screen** to go to the previous page.
- **Tap the right ~25% of the screen** to go to the next page.
- **Tap the middle** to show or hide the reading controls: a progress bar
  at the bottom (drag it to jump to any point in the book) and buttons at
  the top for Settings and back to your library.
- **Press the system back button:** if controls are showing, this hides
  them; otherwise it takes you back to the library.
- **Long-press a word** to select it, then choose an action from the popup
  menu: **Dictionary**, **Wikipedia**, or **✨ Explain the context** (if AI
  is available on your device — see below).
- Tap the **✨ summary icon** in the reading controls for a two-part recap:
  a quick "here's where you left off" reminder of the last few pages, and
  an optional full recap of everything up to your current position.

Nothing you haven't read yet is ever used to generate a lookup or summary,
even if the book is famous enough that the AI would otherwise "know" how it
ends.

### Settings

Reachable from the library or from the top-right icon while reading:

- **Text size** and **font** — apply instantly to the open book.
- **Theme** — Light, Dark, or Sepia, applied to both the book and the app.
- **Books folder** — change which folder tehuti reads from.

### Closing the app

There's nothing to save manually — your position in every book is written
to disk as you read, so you can just switch away, swipe the app out of
Recents, or turn off the screen at any time. Reopening the book (even after
a full reboot) resumes exactly where you left off.

### AI features (optional)

The "Explain the context" and "summarize" features use on-device AI
(Gemini Nano) when your phone supports it. On devices/emulators that don't,
the AI menu items and icon simply don't appear — everything else in the app
works the same either way. Developers building from source can also
configure a cloud fallback; see below.

---

## Building from source

There's no published build to download yet — this section is for anyone
building and running the app themselves.

- **Package:** `com.tehuti.reader`
- **Min SDK:** 26 · **Target/compile SDK:** 37
- **UI:** Jetpack Compose (Material 3), single Activity + Navigation-Compose
- **Stack:** Hilt, Room, DataStore, Retrofit, Coil, [Readium Kotlin
  Toolkit](https://github.com/readium/kotlin-toolkit) 3.3.0

### Prerequisites

- Android Studio (used here for its bundled JDK and SDK)
- An Android SDK checkout at `$HOME/Library/Android/sdk` (default on macOS
  when installed via Android Studio)
- At least one emulator (AVD), or a physical device with USB debugging on

Set these before running any command below:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

`JAVA_HOME` must point at Android Studio's bundled JDK — the project uses
AGP's built-in Kotlin support, which is picky about JDK version.

### Build

```bash
./gradlew :app:assembleDebug   # build only
./gradlew :app:installDebug    # build + install on a running/connected device
```

### Quickest way to get a working install: `scripts/dev-reset.sh`

This does a full clean cycle in one shot: kills any running emulator,
rebuilds, boots the `Pixel_10` AVD, uninstalls any previous build (avoids
stale-database crashes), installs, pushes everything in `./books` to
`/sdcard/Books` on the device, and launches the app.

```bash
./scripts/dev-reset.sh
```

Since it's a fresh install, you'll need to grant `/sdcard/Books` via the
in-app folder picker once it launches (see "Adding books" above).

### Manual build/install/launch

```bash
"$ANDROID_HOME/emulator/emulator" -list-avds
"$ANDROID_HOME/emulator/emulator" -avd <avd_name> &

"$ANDROID_HOME/platform-tools/adb" wait-for-device
until [ "$("$ANDROID_HOME/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

./gradlew :app:installDebug && \
  "$ANDROID_HOME/platform-tools/adb" shell am start -n com.tehuti.reader/.MainActivity
```

A software-rendered AVD (`hw.gpu.enabled=no`) tends to be the most stable
for this project — some GPU-accelerated system images have shown flaky
WebView rendering with Readium's EPUB navigator.

`installDebug` reinstalls **over** existing app data. If the local database
schema changed since your last install, the app can crash on launch with a
Room identity-hash error. Avoid this with a clean reinstall:

```bash
"$ANDROID_HOME/platform-tools/adb" uninstall com.tehuti.reader 2>/dev/null
./gradlew :app:installDebug && \
  "$ANDROID_HOME/platform-tools/adb" shell am start -n com.tehuti.reader/.MainActivity
```

A clean uninstall also wipes the granted Books-folder permission and all
reading progress, so you'll need to re-grant the folder afterward.

### Pushing test books to the emulator

```bash
"$ANDROID_HOME/platform-tools/adb" shell mkdir -p /sdcard/Books
"$ANDROID_HOME/platform-tools/adb" push "/path/to/book.epub" /sdcard/Books/
```

Then grant `/sdcard/Books` via the in-app folder picker, or pull-to-refresh
the library if it's already granted (the scan runs on grant or manual
refresh, not on a live filesystem watch).

### Cloud AI fallback (optional, for development)

On-device AI (Gemini Nano) only works on supported hardware, so the emulator
and most non-flagship devices report it as unavailable. To exercise the AI
features anyway, configure a cloud fallback:

```bash
cp .env.default .env
# then edit .env and fill in CLOUD_AI_API_KEY
```

`.env` is gitignored — never commit it. Without it, the app degrades
gracefully to "AI features unavailable" wherever on-device AI isn't
supported, which is expected and not a bug.

### Troubleshooting

- **Blank/white reader screen, or the emulator won't render the WebView:**
  try a different AVD/system image — some preview images have GPU-related
  WebView issues. A software-rendered image is the safest fallback.
- **`adb: no devices/emulators found`:** no emulator is running. Start one
  (see above) or check `adb devices`.
- **App crashes on launch with a Room identity-hash error:** stale database
  from a previous install — do a clean uninstall/reinstall (see above).
