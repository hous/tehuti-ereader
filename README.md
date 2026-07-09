# tehuti

A minimalist e-book reader for Android. Point it at a folder of EPUB files
and read. No accounts, no store, no clutter.

- Full-screen, distraction-free reading
- A spoiler-safe AI companion that can recap the story so far or explain
  a character or term, using *only the pages you have already read*
- Every book reopens exactly where you left off
- Adjustable font, text size, and Light/Dark/Sepia themes
- Long-press any word for a dictionary or Wikipedia lookup

## Motivation

I read in bed, and I regularly fall asleep mid-chapter. The next night I
open the book and have no idea where I left off or what was happening.
tehuti's summary feature fixes that: one tap gives me a short "here's where
you left off" recap so I can get back into the story immediately.

The same goes for characters. Halfway through a novel I'll hit a name and
think "wait, who is this again?" I want a reminder, but I don't want a
search result that casually reveals the ending. So the AI features in
tehuti are built around a hard rule: they only ever see the text you have
actually read. Recaps and explanations never draw on later chapters, or on
outside knowledge of the book, even if it's famous. No spoilers, ever.

## Using the app

**Add books.** On first launch, pick the folder where your EPUB files live.
Drop books into it however you like (file manager, USB, cloud sync), then
pull down on the library to refresh. Files must be DRM-free EPUBs. Check out 
[Project Gutenberg](https://www.gutenberg.org/)!

**Read.** Tap the right side of the screen for the next page, the left for
the previous one, the middle to show controls. The bottom bar seeks through
the book; the top bar has Settings and your library.

**Look things up.** Long-press a word and choose Dictionary, Wikipedia, or
"✨ Explain" for a spoiler-safe AI summary.

**Catch up.** Tap the sparkle icon in the controls for a quick recap of the
last few pages, with an optional full recap of everything up to your
bookmark.

**Adjust.** Settings covers text size, font, theme, and which folder your
books live in. Designed to be easy on the eyes for night-time reading, tehuti
aims to minimize blue light and allows you to customize this to fit your 
device's settings.

There is nothing to save or close. Your position is stored as you read.

## For developers

Built with Kotlin, Jetpack Compose, and the
[Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit).
Min SDK 26, target SDK 37.

### Build and run

Requires Android Studio (for its bundled JDK and the Android SDK):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew :app:assembleDebug   # build
./gradlew :app:installDebug    # build + install on a connected device
```

Two scripts do a full clean cycle (rebuild, uninstall, install, push the
`./books` folder to the device, launch):

```bash
./scripts/dev-reset.sh      # emulator (boots the Pixel_10 AVD)
./scripts/device-reset.sh   # physical Pixel 7 over wireless debugging
```

Both wipe app data, so you'll need to re-grant the books folder in the app
afterward. To push books manually:

```bash
adb shell mkdir -p /sdcard/Books
adb push book.epub /sdcard/Books/
```

### AI configuration

The AI companion runs on-device (Gemini Nano) where supported. On other
devices, including emulators, the AI features hide themselves unless you
configure the cloud fallback:

```bash
cp .env.default .env   # then fill in CLOUD_AI_API_KEY
```

`.env` is gitignored. Never commit it.

### Troubleshooting

- Blank reader screen on an emulator: use a software-rendered AVD. Some
  GPU-accelerated images have flaky WebView rendering with Readium.
- Room identity-hash crash on launch: stale database from a previous
  install. Uninstall the app and reinstall.

## License

tehuti is source-available under the
[PolyForm Noncommercial License 1.0.0](./LICENSE.md). You can use, modify,
and share it freely for any noncommercial purpose. Commercial use is not
permitted.

Required Notice: Copyright (c) 2026 hous (https://github.com/hous/tehuti-ereader)
