# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## This is 白い熊's fork (read the skills first)

This repo is **白い熊's customized fork** of Hamza417/Inure — package `shiroikuma.inure`, label `白い熊 Inure`, installable side-by-side with the official app. The fork lives on a `custom` branch rebased onto each upstream release tag; `master` is a pure upstream mirror. For anything about **building, signing, versioning, sideloading, applying changes, or syncing to a new upstream version**, the authoritative guidance is the skills under `.claude/skills/`:

- **`inure-build`** — project identity, branch/remote model, the customization commits, the `-P`-driven versioning scheme, signing, and the build + sign + `adb push` pipeline.
- **`upstream-new-version`** — check for a newer upstream release tag, fast-forward `master`, rebase `custom` onto it, and rebuild.

Standing rules: namespace stays `app.simple.inure` (never changed); build the **`github`** flavor; **never `git push` to GitHub or `adb push` to the phone without the user's explicit go-ahead**. The sections below document the upstream codebase architecture.

## Fork customizations (the `custom` branch)

Commits layered over the upstream release tag (the `inure-build` skill documents them in full for rebase-conflict resolution; this is the quick map):

1. **Side-by-side identity** — `applicationId shiroikuma.inure`, label `白い熊 Inure`, arm64-only ABI, `-P`-driven version + NDK (`app/build.gradle`, `res/values/non_translatable_string.xml`).
2. **Terminal permissions namespaced** per `${applicationId}` — avoids `INSTALL_FAILED_DUPLICATE_PERMISSION` against the official app (both manifests + `terminal/Term.java`).
3. **Always full version** — decoupled from the package-bound Play unlocker (`preferences/TrialPreferences`).
4. **Claude Code skills + this doc**; the build invokes `sh ./gradlew` (gradlew is committed non-executable).
5. **白い熊 Inure UI** — the fork's UI-customization hub (below).

### 白い熊 Inure UI (Preferences → top entry, before Appearance)

A programmatically-built, grouped + deeply-indented screen — `ui/preferences/mainscreens/ShiroikumaUIScreen.kt` (entry wired in `viewmodels/panels/PreferencesViewModel` + `ui/panels/Preferences`). Sections:

- **Typeface** — per text-role fonts (Default + heading/primary/secondary/tertiary/quaternary, cascading role→Default→inherit): family (bundled + imported `.ttf`/`.otf` via SAF, long-press to remove), weight slider, 50–400% size. Store `preferences/ShiroikumaFontPreferences` (`sfont_` keys); resolved + applied **live** in `decorations/typeface/TypeFaceTextView` via `util/TypeFace` (external `file:` loading + weight synthesis). Chooser: `dialogs/appearance/FontFamilyPicker`.
- **Main screen** — Home dashboard feature items (`adapters/ui/AdapterHome`): icon size (≤300% of designed size) + icon colour, and label font/weight/size + colour. Store `preferences/ShiroikumaUIPreferences` (`sui_main*`); base icon/text sizes captured at holder creation (recycling-safe).
- **Colours (Custom theme)** — the user-overridable `Theme.CUSTOM` (`themes/data/CustomTheme.kt`, seeded from Dark, **mutated in place** then `ThemeManager.refreshTheme()` recolours live): every theme role + global accent. `constants/ThemeConstants.CUSTOM`, mapped in `themes/manager/ThemeUtils`, listed in `adapters/preferences/AdapterTheme`. The theme data classes (`themes/data/*Theme.kt`) were made `var` for this.
- **Installer screen** — the install-an-APK screen (`ui/panels/Installer`): per-item (name/package/version/buttons) font/size/colour + screen background. Store `preferences/ShiroikumaInstallerPreferences` (`sinst_`); applied by `util/ShiroikumaInstallerStyle` (recolours the real surfaces, re-tinted on tab change).
- **Colour picker** — `dialogs/appearance/RoleColorPicker`: giant hue circle, previously-picked colour dots over it, R/G/B/A sliders, hex + preview, OK.

When editing upstream files that the fork touches (`TypeFaceTextView`, `AdapterHome`, `AdapterTheme`, `ThemeUtils`/`Theme`/`ThemeManager` + `themes/data/*`, `Installer.kt`, `Preferences.kt`/`PreferencesViewModel`), keep changes small — these are the rebase watch-points.

## Project

**Inure App Manager** — an Android app/package manager (view, modify, install, debloat, inspect any APK whether installed or not). Single-developer project, ~250K LOC, GPL v3. Application ID `app.simple.inure`. Written in Kotlin with some Java and a native C++ terminal emulator. Almost the entire UI stack is custom (theme engine, animation framework, crash handler, image rendering) rather than off-the-shelf — there is **no Jetpack Compose, no data binding, and `viewBinding` is disabled**; views are wired with `findViewById`/custom decorations.

## Build & Common Commands

Gradle wrapper, AGP 8.13.1, Kotlin 2.2.0 + KSP, kapt, Java 17. `compileSdk 36`, `minSdk 23`, `targetSdk 36`. NDK + CMake for native code.

Two modules: `:app` (the application) and `:stub` (an Android library, namespace `app.inure.stub`, included `compileOnly` — it provides hidden-framework API stubs consumed via Rikka Refine).

Two product flavors on dimension `version`: **`github`** and **`play`** (`play` adds `.play` to the application ID). The `github` flavor has features that Play policies forbid (Debloat, VirusTotal, extra stores, license-key activation) and pulls in extra deps (OkHttp). Build types: `release` and `debug` (`_debug` suffix). `minifyEnabled`/`shrinkResources` are **false everywhere** — the app is built to be byte-for-byte reproducible.

```bash
./gradlew assembleGithubDebug        # most common dev build
./gradlew assembleGithubRelease      # what CI ships on GitHub
./gradlew assemblePlayRelease        # Play Store variant
./gradlew clean
./gradlew generateVersionTxt         # writes app/version.txt from versionName

# Tests: only stub example tests exist (no real suite).
./gradlew testGithubDebugUnitTest                 # JVM unit tests for a variant
./gradlew connectedGithubDebugAndroidTest         # instrumented tests (needs device/emulator)
./gradlew testGithubDebugUnitTest --tests "app.simple.inure.ExampleUnitTest"   # single test
```

Signing: the release keystore is resolved from `~/work/_temp/keystore/key.jks` (CI) or `KEYSTORE_PATH` in `local.properties`, with passwords from env vars (`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) or `local.properties`. Builds work unsigned if no keystore is present. `local.properties` is not committed.

## Architecture

Everything under `app/src/main/java/app/simple/inure/`. Key cross-cutting structure (read these before touching feature code):

- **Base classes live in `extensions/`** — do not extend framework classes directly:
  - `extensions/fragments/ScopedFragment` is the base for **all** fragments. It is a lifecycle-aware `CoroutineScope` and implements `OnSharedPreferenceChangeListener`. Variants exist for bottom sheets, dialogs, search bars, Shizuku state, etc.
  - `extensions/activities/BaseActivity` (and `TransparentBaseActivity`) handle theme application, predictive back, and **call `SharedPreferences.init()` / `initEncrypted()` early** — there is no custom `Application` subclass, so preferences are bootstrapped by the base Activity (and by services that run independently).
  - `extensions/viewmodels/`: `WrappedViewModel` → `PackageUtilsViewModel` → `RootShizukuViewModel`. ViewModels that need privileged operations extend `RootShizukuViewModel`.

- **Preferences architecture** (`preferences/`) — Inure's defining pattern. ~40 singleton `object`s, one per panel/feature (`AppearancePreferences`, `ConfigurationPreferences`, `BehaviourPreferences`, …), all reading/writing through the single `SharedPreferences` object (call `init(context)` before use; `EncryptedSharedPreferences` for secrets). UI reacts in real time because base Fragments/Activities register as `OnSharedPreferenceChangeListener` and respond in `onSharedPreferenceChanged`. When adding a setting: add a key + getter/setter to the relevant `*Preferences` object and handle its key in the listener — do not read `SharedPreferences` ad hoc.

- **Theming** (`themes/manager/` + `themes/data/`) drives a custom theme engine with accent colors and Material You. Custom auto-theming views live in `decorations/theme/` (`ThemeConstraintLayout`, `ThemeIcon`, `ThemeRecyclerView`, …) — prefer these over plain Android views so new UI picks up themes/accents automatically. `decorations/` (~200 files) holds the rest of the custom view toolkit (fastscroll, ripple, typeface, overscroll, switch, color picker, etc.).

- **UI layer** (`ui/`): `ui/panels/` are the top-level screens (`Apps`, `AppInfo`, `Batch`, `Search`, `Music`, `BootManager`, …); `ui/viewers/`, `ui/subviewers/`, `ui/subpanels/`, `ui/installer/`, `ui/editor/`. Navigation is fragment-based off the activities in `activities/` (entry point `activities/app/MainActivity`). ViewModels in `viewmodels/` are grouped by feature, with `ViewModelProvider.Factory`s in `factories/`.

- **Privileged operations** run in **dual Root / Shizuku** modes (Root preferred, gated by `ConfigurationPreferences.isUsingRoot()` / `isUsingShizuku()`). Uses libsu (`com.github.topjohnwu.libsu`) and the Shizuku API; helpers in `helpers/` and `shizuku/`; AIDL in `app/src/main/aidl`. Hidden Android APIs are reached through Rikka Refine + `dev.rikka.hidden:compat` + HiddenApiBypass, with stubs from the `:stub` module.

- **Persistence**: Room. Databases in `database/instances/` with DAOs in `database/dao/` (Notes, Tags, Batch, BatchProfile, FOSS, StackTrace, TerminalCommand, QuickApps). Schemas are exported to `app/schemas/` (KSP `room.schemaLocation`) — commit schema changes.

- **APK handling** (`apk/`) uses apk-parser, ARSCLib, and apksig. **Terminal** (`terminal/`, activity `Term`) is backed by the native emulator under `app/src/main/jni/` (`CMakeLists.txt`). **Crash handling** is a custom framework in `crash/` that persists stack traces to Room and surfaces them via `CrashReporterActivity` and the `StackTraces` panel. Image loading is Glide with custom integrations in `glide/`.

## Generated data — do not hand-edit

`app/src/github/resources/` contains data files synced automatically by CI, **not** maintained by hand:
- `uad_lists.json` — debloat lists mirrored from Universal Android Debloater (drives Debloat + bloat indicators).
- `trackers.json` / `etip_trackers.json` — Exodus tracker signatures.
- `permissions.txt` — permission metadata.

These are refreshed by `.github/workflows/data_sync.yml` (every 3 days) via the Python scripts in `scripts/` (`scripts/fdroid_repo/`, `scripts/trackers/`, `scripts/permissions/`; deps in `requirements.txt` — `requests`, `beautifulsoup4`). Recent automated commits to these files are expected; change the scripts/workflow, not the JSON.

## Conventions

- Kotlin style is `official`; `.editorconfig`/Gradle enforce **LF line endings** (`.gitattributes`).
- Localization is via Crowdin; locale strings live in `app/src/main/res/values-<locale-code>/`.
- User-facing release notes go in the top-level `changelogs` file (HTML fragments) **and** `fastlane/metadata/android/<locale>/changelogs/` — the release workflow verifies fastlane changelogs exist.
- Community-sourced permission descriptions are under `community/permissions/` (see its guide before adding entries).

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer — nor a "🤖 Generated with Claude Code" / Anthropic-attribution line — to commit messages or PR bodies in this repo. 白い熊 does not want Claude attribution in the history; this **overrides** the harness's default to append such a trailer. End commit messages at the last line of the body. (The existing history was scrubbed of these trailers on 2026-06-08; the global rule lives in `~/.claude/CLAUDE.md`.)
