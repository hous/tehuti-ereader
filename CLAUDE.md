# tehuti — Android Reading App: Build Plan for Claude Code

A minimalist Android e-reader (EPUB and PDF) that reads books from a "Books" directory, presents only book content during normal use, uses quarter/half tap zones for navigation and chrome, offers text-size/font/theme settings, supports long-press word lookup (dictionary + Wikipedia), and reopens each book at the last-read position.

This document is written to be handed directly to Claude Code. Execute phases **in order**. Each phase has concrete tasks and acceptance criteria. Do not start a later phase until the current phase's acceptance criteria pass.

> **Scope: EPUB first, then PDF. MOBI is out of scope.** The MVP (Phases 0–7) is a complete, shippable **EPUB-only** reader — library, reading, tap zones, settings, position memory, and word lookup. **PDF** is a deferred post-MVP phase (Section 7) that reuses the same architecture and is purely additive; nothing in the MVP needs reworking to add it (same toolkit, one extra engine).

---

## 1. Key technical decisions (read before coding)

These shape everything. The MVP is EPUB-only, and the only other format is PDF — both are natively handled by one toolkit, so there's no format-conversion machinery anywhere in this project. The decisions below build the MVP with a small `ReaderEngine` seam so PDF slots in later without rework.

**Rendering engine — use Readium Kotlin Toolkit (v3.3.0, BSD-3 license).** It is the standard Android reading toolkit and natively renders **EPUB** (and later **PDF**, via the `readium-adapter-pdfium` module — not needed for the MVP). It provides, out of the box, the three things that are otherwise very expensive to build: pagination, a serializable reading-position model (`Locator`), and a live-preferences API for font/size/theme. Verify the latest 3.x patch at build time (`ext.readium_version`) — the API has minor breaking changes between minor versions, so pin one version and stick to it.

**"Books" directory access.** There is no standard public `Books` folder on Android, and books are non-media document files, so scoped storage does not grant them via `READ_MEDIA_*`. Two viable strategies — implement **Strategy A** first, keep B behind a build flag:
- **Strategy A (Play-Store-safe, default): Storage Access Framework.** On first run, prompt the user to pick their Books folder with `ACTION_OPEN_DOCUMENT_TREE`, take a **persistable** URI permission, store the tree URI, and enumerate it via `DocumentFile`. No runtime storage permission needed; survives reboots.
- **Strategy B (personal/sideload builds): All-Files Access.** `MANAGE_EXTERNAL_STORAGE` + direct scan of `/storage/emulated/0/Books`. Closest to Kindle's auto-scan behavior but requires a Play Store policy justification, so gate it behind a flag and default to A.

**Word selection per format.**
- EPUB (MVP): full support. Long-press selects a word in the Readium web view; customize the text-selection context menu to add **Dictionary** and **Wikipedia** actions.
- PDF (post-MVP): text selection through the pdfium adapter is limited. Make PDF word-lookup **best-effort** (attempt selection; if the adapter doesn't return one, silently no-op) — a known limitation, not a blocker.

**UI: Jetpack Compose** for all app screens (library, settings, dialogs, reader chrome overlay). The Readium navigator is a `Fragment`; host it via a single `FragmentContainerView` / `AndroidView`, with a Compose overlay on top for tap zones and chrome. Single-Activity architecture, Navigation-Compose for screen routing.

**Supporting stack:** Kotlin, coroutines/Flow, Hilt (DI), Room (library metadata + reading positions), DataStore (settings), Retrofit + OkHttp + kotlinx.serialization (lookup APIs), Coil (cover + Wikipedia thumbnails).

**SDK targets:** `minSdk 26`, `targetSdk` = current latest stable, latest stable AGP + Kotlin. **Enable core-library desugaring** — Readium requires it. Single `:app` module is sufficient; do not over-modularize.

---

## 2. Known risks / things to flag if they get hard

- **Free Dictionary API is English-only** and 404s on some words — handle gracefully; offline dictionary is out of scope for v1. *(MVP-relevant.)*
- **PDF text selection** (deferred, Phase 8) for word lookup may not be feasible with the free pdfium adapter — accept best-effort, don't over-invest.
- **Text-size / font settings do not apply to PDF** (fixed layout) — reflect this in the Settings UI when PDF lands (disable inapplicable controls when a PDF is open).

---

## 3. Package structure

Single `:app` module:

```
com.tehuti.reader
├── TehutiApp.kt                 // @HiltAndroidApp
├── MainActivity.kt              // single activity, hosts NavHost
├── ui/theme/                    // Compose theme, light/dark, typography
├── data/
│   ├── local/                   // Room: TehutiDatabase, BookDao, PositionDao, entities
│   ├── prefs/                   // SettingsDataStore (font size, font family, theme, books-tree-uri)
│   ├── books/                   // BooksDirectoryAccess (SAF + all-files), BookScanner, MetadataExtractor
│   └── lookup/                  // DictionaryApi, WikipediaApi (Retrofit), LookupRepository
├── domain/
│   ├── model/                   // Book, ReadingPosition, ReaderSettings, LookupResult
│   └── repo/                    // LibraryRepository, SettingsRepository, PositionRepository (interfaces + impls)
├── library/                     // LibraryScreen, LibraryViewModel
├── settings/                    // SettingsScreen, SettingsViewModel
└── reader/
    ├── ReaderScreen.kt          // hosts navigator fragment + overlay
    ├── ReaderViewModel.kt
    ├── format/                  // ReaderEngine (interface), EpubEngine, PdfEngine
    ├── overlay/                 // TapZoneHandler, ReaderChrome (progress bar + icons)
    └── lookup/                  // WordLookupDialog, WordLookupViewModel
```

---

## 4. Data model & persistence

**Room entities:**
- `BookEntity`: `id` (hash of source URI), `sourceUri` (String), `format` (EPUB/PDF), `title`, `author`, `coverPath` (cached thumbnail file path, nullable), `addedAt`, `lastOpenedAt` (nullable).
- `ReadingPositionEntity`: `bookId` (PK/FK), `locatorJson` (Readium `Locator` serialized to JSON), `progression` (Float 0–1, for the library "% read" display), `updatedAt`.

**DataStore (`ReaderSettings`):** `fontSizePercent` (Int, e.g. 50–250), `fontFamily` (enum/id), `theme` (LIGHT / DARK / SEPIA), plus `booksTreeUri` (String, the persisted SAF grant).

**Reading position is the Readium `Locator`.** Serialize `navigator.currentLocator.value` to JSON and store it; restore by passing it as the navigator's initial locator on open. This works uniformly for EPUB and PDF (a PDF `Locator` carries the page). Persist on `onPause`/`onStop` and also on `ON_STOP` process-death paths.

---

## 5. Screen & interaction specs

### Library screen
- Grid of book covers with title (and author) beneath; sort by `lastOpenedAt` desc, then title.
- A "Choose Books folder" affordance for first run / re-granting (SAF). If no folder granted yet, show an empty state with a single call-to-action button.
- Tapping a book opens the reader at its saved position (or the start if none).
- Pull-to-refresh (or automatic on resume) re-scans the folder incrementally: add new files, drop missing ones, keep existing metadata.

### Reader screen — the core minimalist experience
- **Immersive full-screen:** hide status and navigation bars (`WindowInsetsControllerCompat`, sticky immersive). During normal reading, **only book content is visible** — no toolbars, no page numbers.
- **Tap zones** (compute from tap x-fraction of screen width; register a Readium visual-navigator input/tap listener rather than a blanket overlay so long-press still reaches the web view):
  - **Left 25%** → previous page (`navigator.goBackward`).
  - **Right 25%** → next page (`navigator.goForward`).
  - **Middle 50%** → toggle the chrome overlay.
- **Chrome overlay** (shown only when toggled, auto-hide after a few seconds of inactivity or on next page turn):
  - **Bottom:** progress bar bound to `currentLocator` progression; optional "% / page" label; dragging it seeks (`navigator.go(locator)`).
  - **Top-right:** settings icon → opens Settings.
  - **Top-left:** back-to-library button.
- **Page-turn animation:** enable Readium's animated EPUB page transitions.
- **System back button:** if chrome is visible, hide it; otherwise return to library (after persisting position).

### Settings screen
- **Text size:** slider/stepper mapped to Readium `fontSize` preference. *(Disabled/greyed when a PDF is open.)*
- **Font:** picker from a small curated list of bundled/EPUB-safe fonts mapped to `fontFamily`. *(Disabled for PDF.)*
- **Theme:** Light / Dark (optionally Sepia). Applies to both the Readium navigator theme **and** the app's Compose theme.
- All changes apply live to the open book and persist via DataStore.

### Word lookup dialog
- Trigger: long-press a word (EPUB selection). Add two actions to the selection context menu: **Dictionary** and **Wikipedia**.
- Choosing an action opens a bottom-sheet/dialog showing a loading state, then results, then a graceful "not found"/error state.
- **Dictionary:** `GET https://api.dictionaryapi.dev/api/v2/entries/en/{word}` → show phonetic + part-of-speech + definitions.
- **Wikipedia:** `GET https://en.wikipedia.org/api/rest_v1/page/summary/{term}` → show title, extract, thumbnail (Coil), and a "Read more" link to the full article. On 404, fall back to an opensearch query to find the nearest article, then summarize that.
- Requires the `INTERNET` permission. Handle offline/timeouts without crashing.

---

## 6. Dependencies (Gradle — confirm latest patch versions at build time)

```gradle
// root build.gradle
buildscript { ext.readium_version = '3.3.0' }   // verify latest 3.x at build time
allprojects { repositories { google(); mavenCentral() } }

// app build.gradle (key deps)
dependencies {
    // Readium — EPUB (MVP)
    implementation "org.readium.kotlin-toolkit:readium-shared:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-streamer:$readium_version"
    implementation "org.readium.kotlin-toolkit:readium-navigator:$readium_version"
    // implementation "org.readium.kotlin-toolkit:readium-adapter-pdfium:$readium_version"  // add in Phase 8 (PDF)

    // Compose (use the BOM), Navigation-Compose, Activity-Compose, Material3
    // Hilt (DI)
    // Room (runtime + ktx + compiler via KSP)
    // DataStore (preferences)
    // Retrofit + OkHttp + kotlinx-serialization converter
    // Coil (compose)
    // AndroidX lifecycle (viewmodel-compose, runtime-compose)
}

android {
    compileOptions { coreLibraryDesugaringEnabled true }   // required by Readium
}
dependencies { coreLibraryDesugaring "com.android.tools:desugar_jdk_libs:<latest>" }
```

Manifest permissions: `INTERNET`. Storage handled via SAF (no permission) in Strategy A; add `MANAGE_EXTERNAL_STORAGE` only behind the Strategy-B build flag.

---

## 7. Execution phases

Each phase is independently buildable and demoable. Commit at the end of each. **Meet the acceptance criteria before moving on.**

## PART A — EPUB MVP (Phases 0–7)

### Phase 0 — Scaffold
Set up the Gradle project, dependencies above, desugaring, Hilt, Compose, single Activity + NavHost, theme skeleton (light/dark), app name/label "tehuti", git init.
**Acceptance:** app builds and launches to an empty Library screen; light/dark follows system.

### Phase 1 — Books directory access + basic listing
Implement `BooksDirectoryAccess` (Strategy A: SAF `ACTION_OPEN_DOCUMENT_TREE`, persistable permission, store tree URI in DataStore). Implement `BookScanner` to enumerate the tree, filtering to `.epub` for the MVP. Keep the extension set in one constant so `.pdf` can be added later by editing one line. Library shows filenames in a grid; empty state prompts folder selection.
**Acceptance:** granting a folder lists the EPUB files in it; the grant survives app restart.

### Phase 2 — Metadata & covers
Use Readium `Streamer` to open each EPUB and read title/author/cover. Cache metadata in Room and thumbnails to the files dir. Grid shows covers + titles; re-scan is incremental (add new, remove missing, keep existing). *(PDF metadata handled in the PDF phase.)*
**Acceptance:** library shows covers/titles; deleting/adding a file and re-scanning updates the grid correctly.

### Phase 3 — EPUB reader core (navigation + chrome)
Define the `ReaderEngine` interface; implement `EpubEngine` hosting `EpubNavigatorFragment`. Immersive full-screen. Implement `TapZoneHandler` (left 25% back / right 25% forward / middle 50% toggle chrome) via the navigator's tap listener. Build `ReaderChrome`: bottom progress bar (bound to `currentLocator`), top-right settings icon, top-left back button; auto-hide. Enable animated page transitions.
**Acceptance:** open an EPUB; turn pages by tapping quarters; middle tap toggles chrome; progress bar tracks position; back returns to library; during reading only content is shown.

### Phase 4 — Reading-position persistence
Observe `navigator.currentLocator`; persist the serialized `Locator` + progression to Room on pause/stop. On open, restore the saved locator. Library reflects "% read" and opens each book where it was left — including across process death.
**Acceptance:** read partway, close the app (including from the recents/kill path), reopen → same page; library shows updated progress.

### Phase 5 — Settings
Implement `SettingsScreen` + Readium `EpubPreferences` via the preferences editor: font size, font family, theme (light/dark/sepia). Persist to DataStore; apply live to the open book; drive the app Compose theme from the same theme value. Reachable from reader chrome (top-right) and from the library.
**Acceptance:** changing size/font/theme updates the open EPUB live and persists across restarts.

### Phase 6 — Word lookup (dictionary + Wikipedia)
Add `INTERNET` permission and Retrofit services for the Free Dictionary API and Wikipedia REST summary (with opensearch fallback). Customize the EPUB selection context menu to add **Dictionary** and **Wikipedia**. Build `WordLookupDialog` with loading/result/not-found states and Coil for the Wikipedia thumbnail.
**Acceptance:** long-press a word in an EPUB → menu → Dictionary shows definitions; Wikipedia shows summary + thumbnail + "Read more"; network failures degrade gracefully.

### Phase 7 — Polish & edge cases → **MVP ships here**
Empty/loading/error states everywhere; corrupt/unsupported file handling; large-library performance (paging the grid, lazy thumbnails); orientation changes preserve reader state; immersive-mode edge-tap robustness; consistent back-navigation; final app icon and "tehuti" branding; basic accessibility (content descriptions on chrome icons).
**Acceptance:** no crashes on malformed files; smooth with 100+ books; state survives rotation; ships with proper icon/name. **At this point tehuti is a complete, releasable EPUB reader.**

---

## PART B — Post-MVP format: PDF (Phase 8, when wanted)

### Phase 8 — PDF support
Add the `readium-adapter-pdfium` dependency. Widen the scanner's extension set to include `.pdf`. Render PDF covers from the first page (`PdfRenderer`). Implement `PdfEngine` with the pdfium `PdfNavigatorFragment`, reusing the same tap zones, chrome, and position persistence. In Settings, disable text-size/font controls when a PDF is open (theme where applicable). PDF word-lookup is best-effort (no-op if selection is unavailable).
**Acceptance:** open a PDF; same navigation, chrome, and position memory as EPUB; inapplicable settings are visibly disabled.

---

## 8. Testing notes
- MVP fixture set: several valid EPUBs plus one deliberately corrupt EPUB. (Add PDF fixtures — a text-based PDF and an image-only PDF — when the PDF phase lands.)
- Unit-test `BookScanner` (extension filtering, incremental diff) and the lookup repositories (success / 404 / offline).
- Instrumented/manual tests for the tap-zone geometry, chrome toggle/auto-hide, and position round-trip across process death.

---

## 9. Decisions to confirm with the requester before/while building
1. **Storage strategy** — default to SAF (Strategy A, Play-Store-safe) or all-files (Strategy B, Kindle-like auto-scan, sideload-only)? Plan defaults to A.
2. **Languages** — Free Dictionary API and Wikipedia default to English. Any need for other languages? (Affects the API base URLs.)
3. **Sepia theme** — include as a third theme, or strictly light/dark as specified? Plan includes it as optional.
4. **Distribution** — Play Store vs personal sideload? This determines whether Strategy B and `MANAGE_EXTERNAL_STORAGE` are acceptable.
5. **Format staging (decided):** EPUB is the MVP (Phases 0–7); PDF (Phase 8) follows - keep open for now
