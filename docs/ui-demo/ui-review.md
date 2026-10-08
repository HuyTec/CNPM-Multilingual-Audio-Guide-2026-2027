# Android UI Demo Source Review

Task HVP-22. Reviewed `android/app/src/main` against `docs/en/PRD_Report.md` UC-01–UC-05 and the `android-compose-design` skill references. This is **source inspection only**. Compilation, Preview rendering, device launch, navigation execution, font-scale behavior, TalkBack, and restored-state behavior require separate integration evidence in README.

## Priority findings

No P0 or P1 issue was identified in this scoped source inspection. Remaining P2 improvements:

| Location | Priority | User impact | Feasible improvement |
| --- | --- | --- | --- |
| `android/app/src/main/java/vn/hvp/travelvoice/ui/screens/DetailScreen.kt:115` | P2 resolved | EN/JA sample bodies mixed Vietnamese disclaimer. | Fully localized EN/JA static sample paragraphs with separate FULL/SHORT variants; still clearly demonstration text. |
| `android/app/src/main/java/vn/hvp/travelvoice/ui/components/MapPreview.kt:49` | P2 resolved | Zoom/pan could clip marker touch targets. | Clamp offsets to available bounds minus the 48 dp target, keeping markers reachable. |
| `android/app/src/main/java/vn/hvp/travelvoice/ui/screens/OfflineScreen.kt:100` | P2 | After successful update to 1.1, Check correctly reports latest. Repeated error demonstration requires resetting the demo or deleting optional content first. | Add an explicit scenario-reset action if repeated presentations require it; preserve installed-version safety. |

Hardcoded Vietnamese screen-body text remains a prototype localization limitation. Fully translated application UI is not claimed.

## Render review fixes

- `android/app/src/main/java/vn/hvp/travelvoice/ui/components/Components.kt:75` - P2 resolved - vector artwork drew outside its layout bounds and into nearby content; clip to the themed image shape.
- `android/app/src/main/java/vn/hvp/travelvoice/ui/theme/Theme.kt:16` - P2 resolved - default elevated surface roles produced purple offline cards inconsistent with the product palette; define light/dark surface container roles explicitly.
- `android/local.properties:1` - verification blocker resolved - Windows SDK drive separator was not escaped; local SDK path now uses `E\:/...`. This machine-only file remains ignored.

## Verification method and limits

Final verification on 2026-10-07 completed with `BUILD SUCCESSFUL`:

- `:app:assembleDebug`: passed; installable debug APK generated.
- `:app:testDebugUnitTest`: 5 tests passed, 0 failures/errors. Covers Explore → Detail → Player with language/mode retained, manual search without GPS, offline cancellation/deletion protection, matching map marker navigation at 320 dp, and banner navigation without autoplay.
- `:app:assembleDebugAndroidTest`: passed; instrumentation APK compiled, not executed on a device.
- `:app:lintDebug`: passed, 0 errors, 6 warnings and 2 informational hints. Warnings concern pinned dependency versions and Android 12 backup configuration; hints suggest primitive float state. No lint suppression added.
- Five final host captures are saved in `previews/` and visually inspected. No remaining P0/P1 source or host-render issue was identified; the scenario-reset P2 above remains.

Verification runs use `-PdemoBuildDir=.verification-build` so they do not overwrite an Android Studio build in the same checkout. SDK/cache/APK/output folders are ignored. Gradle wrapper jar and distribution checksums were verified against official Gradle SHA-256 files.

Host UI checks use Robolectric API 28 and actual MainActivity/Compose semantics. PixelCopy redraw capture timed out on Windows; host images now render the activity decor view into a native Canvas bitmap. This is host rendering, not an emulator screenshot or Android Studio Preview render. Test selectors scroll the LazyColumn before locating uncomposed items.

Screens have Preview declarations; Offline adds dark/font-scale previews. No device is attached (`adb devices` returned an empty list); device launch, connected instrumentation execution, TalkBack, Android Studio Preview rendering, rotation/process recreation and device performance remain unverified. Test APK compilation alone does not establish those results.

Dependencies are deliberately pinned for this demo. Lint may report newer Gradle/AndroidX/Robolectric versions, Android 12 backup configuration and primitive-state boxing suggestions. Do not suppress lint errors or invent production integration to eliminate prototype warnings.

## PRD consistency inspected

| PRD | Source evidence and boundary |
| --- | --- |
| UC-01 | Search matches name/street; categories, 50–500 m radius, sorted static distances, max20 manual/max5 nearby, schematic map tab and marker preview. GPS-denied state retains manual selection paths. Loading, empty and retry/error scenarios are represented. |
| UC-02 | Detail retains location and FULL/SHORT while selecting languages. Missing-translation fixtures show EN/VI fallback notice. Loading and no-content states disable Listen. Guide text is approved editorial demo material, not a publication/database contract. Feedback is local-only with a no-server disclaimer. |
| UC-03 | Player receives matching ID and displayed language/mode. Fake time, seek, speed, pause/resume/completion and prerecorded/TTS-fallback labels are present. Call/headphone-disconnect scenarios pause without resetting position. No audio or TTS service. |
| UC-04 | Base package has no delete action. Update and optional delete have confirmations. Installed version changes only after fake verification succeeds. Cancellation/storage/network/integrity errors retain version; network resume retains progress. No files installed. |
| UC-05 | Valid banner click opens nearest fixture by ID, without autoplay. Current app language supplies VI/EN/JA title/message. GPS-denied suppresses banner; notification-denied permits foreground banner; cooldown suppresses delivery; invalid ID produces notice without routing. |

## Compose and scope inspection

Navigation Compose uses small route arguments, single-top tabs and saved tab state. Screens use `rememberSaveable`; timers use composition-scoped `LaunchedEffect`. No service, API client, database, persistence repository, ViewModel, DI, actual GPS/geofence/permissions/notifications, audio/TTS or download manager was found in source/manifest. Manifest declares no network/location/storage/notification permissions.

Material controls provide selected/action semantics; meaningful icon-only controls have descriptions. Repeated location cards use stable IDs. State messages use polite live regions; long content scrolls. Offline declares light/dark/large-font previews and primary screens/components declare content previews. Declarations alone do not prove rendered layout or accessibility behavior.
