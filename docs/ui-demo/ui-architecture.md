# Android UI Demo Architecture

## Baseline

HVP-22 implements a standalone native Kotlin/Jetpack Compose prototype under the existing `android/` directory. At initial inspection this directory contained only `README.md`, with no Android Gradle module to preserve. The React web admin remains separate. Canonical requirements are `docs/en/PRD_Report.md`; `docs/PRD.md` is absent. Detailed traceability is in `ui-requirements.md` alongside this file.

Use the available `android-compose-design` skill and its Android UI / Compose engineering references. Keep dependencies to the Gradle Android/Kotlin setup, Compose Material 3, activity integration, Navigation Compose if chosen by the foundation, and standard preview tooling. Record exact versions in the actual Gradle files rather than choosing conflicting versions in this design document.

## Screen list and composable hierarchy

```text
MainActivity
└── TravelVoiceTheme
    └── DemoApp / Navigation Compose shell
        ├── ExploreScreen
        │   ├── Search + CategoryChips + Nearby/Radius controls
        │   ├── LocationCard list
        │   ├── NearbyBanner (explicit scenario)
        │   ├── DemoStatePicker
        │   ├── MapPreview (Danh sách / Bản đồ tab)
        │   └── ModalBottomSheet selected-location preview
        ├── DetailScreen
        │   ├── LanguageSelector + DurationSelector
        │   ├── GuideText / Loading / Empty-or-fallback notice
        │   └── Listen action
        ├── PlayerScreen
        │   ├── Progress + elapsed/total
        │   ├── PlaybackControls + SpeedSelector
        │   └── Demo interruption/fallback controls
        └── OfflineScreen
            ├── Base/OptionalPackageCard
            ├── DownloadProgress / ErrorNotice
            └── ConfirmDialog
```

UC-05 uses a banner inside Explore and a scenario picker, not a newly invented notification inbox. Search, Category, Nearby, and the schematic map are combined in Explore. Player is a dedicated destination with ID/language/mode passed explicitly.

## Navigation graph

Actual Navigation Compose routes: `explore`, `offline`, `detail/{id}`, `player/{id}/{language}/{mode}`. Map is an Explore tab, not a separate route. The app-bar Back action and Android Back pop the navigation stack. `DemoApp` holds the current app language in `rememberSaveable`, passes it to Explore banner titles/messages and the initial Detail picker. Detail forwards the actual displayed fallback language to Player without changing the user's current app-language setting.

Explore card, map preview, or a valid banner opens detail by the same fixture ID. Detail retains language/duration for the player. Player Back returns to detail with the selection intact. Detail Back returns to its origin. Tab clicks should not repeatedly stack copies of top-level screens. Invalid IDs show an unavailable notice without navigating to a nonexistent fixture. Navigating from a banner never triggers playback.

## Reusable component list

Reuse LocationCard, state panels, language/duration selectors, NearbyBanner, package cards, confirmation dialogs, playback controls, and scenario controls where they occur repeatedly. Composables accept immutable UI values and event callbacks, plus caller-controlled `Modifier` when useful. Do not create an interface/implementation pair or component abstraction for every one-off row.

## Fake state strategy

`object DemoData` holds static location/script/package fixtures. Small `rememberSaveable` values hold query, category, language, duration, selected route/ID, playback position/speed, progress, and selected scenario. Plain `remember` holds transient sheet/dialog display state where restoration is unnecessary. Either remains local UI state, with no disk storage.

Fake loading/progress can be explicit scenario state transitions or a short composition-scoped `LaunchedEffect` delay. Never register sensors, permissions, platform player, TTS, download receiver, workers, or a background service. No hidden networking or database. Current installed package version stays visible across fake cancellation/error and changes only in a successful UI scenario. Demo restart may reset everything; disclose that limit in README.

Screen composables have independent previews with fixtures and no-op navigation callbacks. The shell coordinates app language; Detail holds selected duration and forwards the displayed language/mode to Player. Player and Offline hold their presentation states locally. No ViewModel, Flow, domain layer or dependency injection is required.

## Expected structure

```text
android/
├── app/build.gradle.kts
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/vn/hvp/travelvoice/MainActivity.kt
├── app/src/main/java/vn/hvp/travelvoice/ui/
│   ├── navigation/DemoApp.kt
│   ├── screens/            # ExploreScreen, DetailScreen, PlayerScreen, OfflineScreen
│   ├── components/         # Components.kt + MapPreview.kt
│   ├── theme/Theme.kt      # TravelVoiceTheme
│   └── mock/DemoData.kt    # static fixtures + DemoLocation
└── app/src/main/res/       # strings, icons/placeholders; no remote image dependency
```

The existing repo supplies reference screenshots at `docs/ui_requirments/`; they are visual guidance, not new functional requirements. Example nearby distances in the screenshot exceed the PRD 500 m upper search radius, so nearby fixtures must follow the PRD rather than copying those displayed values. Placeholder landmarks/maps should not imply real services or navigation.

## UI and verification boundaries

Material 3, Vietnamese UI, fixed product palette unless approved otherwise, accessible labeled controls and minimum 48 dp targets. Apply Scaffold insets once; use lazy/scrolling content for long text and wrap at large font sizes. Define previews for screen content and important empty/error states. Native Android font availability must be considered if carrying the user's Arial preference; do not silently bundle a licensed font file from the host.

Build evidence, emulator/device launch, navigation interaction, preview rendering, font scaling and TalkBack are distinct checks. The final README records checks actually performed and unavailable checks separately. Screenshots alone do not prove GPS, downloads, playback, content integrity or offline installation, all of which are intentionally mocked.
