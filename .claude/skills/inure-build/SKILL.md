---
name: inure-build
description: Build and maintain the user's (白い熊's) customized fork of Inure App Manager for Android (package shiroikuma.inure, label "白い熊 Inure"), installable side-by-side with the official Inure (app.simple.inure). Use this skill any time the user mentions Inure, Inure App Manager, Hamza417/Inure, shiroikuma-inure, shiroikuma.inure, 白い熊 Inure, "the Inure fork", asks to build/rebuild Inure, build the APK, apply a change to Inure, sign or sideload the Inure build, or references their Inure build pipeline. This fork follows the SAME model as the user's AppManager, FUTO Keyboard (futokxkb), Jami, ArcaneChat, FairEmail, Messeji and SimpleX forks: a `master` branch mirroring upstream, a `custom` branch carrying the user's commits rebased onto each upstream release tag, built locally, signed with a stable per-fork keystore, and sideloaded. Sync to a new upstream release is the companion `upstream-new-version` skill. Default to assuming this skill applies when in doubt during a session about the Inure fork.
---

# Inure App Manager — customized fork build skill

白い熊 maintains a customized build of [Inure App Manager](https://github.com/Hamza417/Inure) on Android, built on a Tuxedo OS workstation from their fork (`ShiroiKuma0/shiroikuma-inure`) and sideloaded alongside the official Inure. The fork's purpose: a distinct `applicationId` + label so it installs side-by-side with the official app, plus a place to carry personal feature changes on top of each upstream release.

The fork model is identical to 白い熊's other Android forks (AppManager, futokxkb, Jami, ArcaneChat, FairEmail, Messeji, SimpleX): `upstream` is fetch-only, `master` is a pure mirror of upstream, a `custom` branch carries the user's commits and is rebased onto each new upstream release **tag**, then force-pushed to the fork only when the user says so. Builds are signed with a stable per-fork keystore so reinstalls land in place.

## Operating mode — Claude Code, direct Bash

This skill runs under Claude Code, so Claude executes every step itself with the Bash tool. **Do NOT use the copy-paste shell-block formatting** (the `r()` stderr helper, cyan `>>>` echoes, `read -p` pause gates) that the older claude.ai-era sister skills carry — those were for a human pasting into a terminal. Run commands directly, report results, and use the **AskUserQuestion** UI for the one human gate (adb push). Claude Code's non-interactive shell does **not** source the user's profile, so `JAVA_HOME` / `ANDROID_HOME` must be exported in every build invocation (see the pipeline).

## Project identity

| Item | Value |
|------|-------|
| Upstream repo | `Hamza417/Inure` (remote `upstream`, HTTPS, fetch-only) |
| Fork repo | `git@github.com:ShiroiKuma0/shiroikuma-inure.git` (remote `origin`, SSH — push here) |
| Local working tree | `~/git/shiroikuma-inure` |
| Mirror branch | `master` — mirrors `Hamza417/Inure` master, never carries our changes, fast-forward only |
| Custom branch | `custom` — carries all commits below, rebased onto each upstream release **tag** |
| Custom applicationId | `shiroikuma.inure` (built on the **github** flavor → no suffix) |
| Custom app label | `白い熊 Inure` (`app_name` in `app/src/main/res/values/non_translatable_string.xml`) |
| Java/Kotlin namespace (UNCHANGED) | `app.simple.inure` (R/BuildConfig package, JNI symbols, taskAffinity — never touch) |
| Flavor we build | **`github`** (NOT `play` — `play` appends `.play` and drops GitHub-only features) → task `:app:assembleGithubRelease` |
| Target ABI | `arm64-v8a` only (commit 1 adds `ndk { abiFilters 'arm64-v8a' }`) |
| Custom signing keystore | `~/.android-keystores/inure.jks` (alias `inure`, passphrase `inure123`) |
| Output APK dir (build) | `app/build/outputs/apk/github/release/` |
| Output APK dir (archive) | `~/tmp/` + on-device `/sdcard/tmp/` |
| APK filename | `shiroikuma-inure_<versionName>_arm64-v8a.apk`, where versionName is the tag with the `build` prefix stripped plus the `+N` tail, e.g. `shiroikuma-inure_107.0.2+2_arm64-v8a.apk` (no datetime, no git sha) |
| Build host | Tuxedo OS |
| Build JDK | OpenJDK 21 at `/usr/lib/jvm/java-21-openjdk-amd64` |
| Android SDK | `~/android-sdk`, platform `android-36` + build-tools `36.1.0` |
| NDK | upstream pins `29.0.13599879 rc2` (an **unreleased RC, not installable**) → build with `-PshiroikumaNdk` pointing at the installed `29.0.14206865` (see NDK note) |
| AGP / Kotlin | 8.13.1 / 2.2.0 (KSP + kapt); Gradle via the wrapper |
| Gradle configuration cache | enabled upstream (`org.gradle.unsafe.configuration-cache=true`); our `-P` reads are CC-safe |
| Git submodules | **none** |

Single Android Gradle project, **Groovy** DSL. Modules `:app` (the application) and `:stub` (an `app.inure.stub` library consumed `compileOnly`).

## Side-by-side install is safe (no collisions)

Coexistence is decided purely by `applicationId` (`shiroikuma.inure` vs official `app.simple.inure`). We keep `namespace = 'app.simple.inure'` — it is build-time only (R/BuildConfig package, JNI `Java_app_simple_inure_*` symbols) and never seen by the OS at install. All three manifest `<provider>` authorities use `${applicationId}` (`.provider`, `.shizuku`), so they auto-differ.

**Custom permissions MUST be namespaced (commit 3).** The terminal emulator declares three custom permissions — `RUN_SCRIPT`, `APPEND_TO_PATH`, `PREPEND_TO_PATH`. Upstream hardcodes them as `inure.terminal.permission.*` (no package prefix) in **both** `app/src/main/AndroidManifest.xml` and `app/src/github/AndroidManifest.xml`, and `Term.java` hardcodes the two PATH ones. Two apps can't declare the same permission name, so installing alongside the official app (which already owns them) fails with **`INSTALL_FAILED_DUPLICATE_PERMISSION`**. Commit 3 prefixes all three with `${applicationId}` in both manifests and derives `Term.java`'s constants from `BuildConfig.APPLICATION_ID`, so each package owns its own (`shiroikuma.inure.terminal.permission.*`). This is required for side-by-side install — see commit 3.

(The only hardcoded `app.simple.inure` strings left are `taskAffinity` values — cosmetic, affecting only recents grouping; not worth changing and left untouched for clean rebases.) Do NOT install the fork over official Inure (different signing keys → Android refuses); they coexist as distinct packages.

## Branch / remote model

| Branch | Purpose | Update mode |
|--------|---------|-------------|
| `master` | Mirrors `Hamza417/Inure` master. Never carries our changes. | Fast-forward only |
| `custom` | Carries all commits below. | Rebased onto each upstream release tag |

`origin` = the fork (SSH, push). `upstream` = Hamza417 (HTTPS, fetch only). Inure release tags are bare `buildNN[.x.y]` (e.g. `build107.0.2`, `build106.5.1`). `custom` is rebased onto the chosen release tag. Checking for and syncing to a newer tag is the **`upstream-new-version`** skill.

## Customization commits on `custom`

### Commit 1 — `Customize for shiroikuma side-by-side install`

Two files. This is the only standing customization so far; it is the most rebase-sensitive commit, so keep it small and re-anchor (don't fight the merge) if upstream restructures `defaultConfig`.

**`app/build.gradle` `defaultConfig`** — applicationId, arm64 abiFilter, and `-P`-driven version (so the build number and local NDK never churn git history):

```diff
-        applicationId "app.simple.inure"
+        applicationId "shiroikuma.inure"
         minSdkVersion 23
         targetSdkVersion 36
-        versionCode 10702
-        versionName "build107.0.2"
+        // Fork versioning (shiroikuma): the inure-build / upstream-new-version skills inject
+        // -PshiroikumaVersionName / -PshiroikumaVersionCode at the gradlew call (build number N
+        // lives in ~/tmp/.shiroikuma_inure_build). The fallbacks below are only used by a bare
+        // build (e.g. Android Studio) and are intentionally NOT kept in sync with the tracked tag.
+        versionCode((project.findProperty('shiroikumaVersionCode') ?: '107020000').toString().toInteger())
+        versionName(project.findProperty('shiroikumaVersionName') ?: 'build107.0.2+0')
         vectorDrawables.useSupportLibrary = true
+        // Fork: build the native terminal lib for arm64 only -> single deterministic APK, faster CMake.
+        ndk {
+            abiFilters 'arm64-v8a'
+        }
```

**`app/build.gradle`** — make the pinned RC NDK overridable (see NDK note):

```diff
-    ndkVersion = '29.0.13599879 rc2'
+    // Fork: -PshiroikumaNdk overrides the pinned RC NDK with a locally-installed one
+    // (upstream pins an unreleased RC; see the inure-build skill's "NDK" note).
+    ndkVersion = project.findProperty('shiroikumaNdk') ?: '29.0.13599879 rc2'
```

Leave `namespace`, the `play` flavor's `applicationIdSuffix ".play"`, and the `resValue "string", "versionName", versionName` line untouched (the latter reads the value we set, so it keeps working).

**`app/src/main/res/values/non_translatable_string.xml`** — app label:

```diff
-    <string name="app_name" translatable="false">Inure</string>
+    <string name="app_name" translatable="false">白い熊 Inure</string>
```

(The `github` flavor does not override `app_name`, so it inherits this. The `play` flavor has its own copy still reading "Inure"; we don't build `play`, so leave it.)

(Commit 2 on `custom` is `Add Claude Code build/sync skills and fork docs` — the `.claude/skills/` + `CLAUDE.md` tooling, not an app customization.)

### Commit 3 — `Namespace terminal permissions per applicationId`

Required for side-by-side install (see "Side-by-side install" above). Three files. Logically part of commit 1; kept separate for now, fold into commit 1 on a future history cleanup.

- `app/src/main/AndroidManifest.xml` and `app/src/github/AndroidManifest.xml`: every `inure.terminal.permission.X` → `${applicationId}.terminal.permission.X` (the three `<permission android:name=…>` declarations + the `android:permission=…` guard on the remote-script activity). Both manifests must match — the merger keeps any that differ, so a missed one re-introduces the collision.
- `app/src/main/java/app/simple/inure/terminal/Term.java`: the two `PERMISSION_PATH_*` constants → `app.simple.inure.BuildConfig.APPLICATION_ID + ".terminal.permission.…"` so the runtime value matches the manifest's substituted `${applicationId}.*`. (`BuildConfig.APPLICATION_ID` is `shiroikuma.inure`; the `inure.terminal.broadcast.*` **action** strings are left alone — actions don't collide.)

Symptom if this regresses on a rebase: `INSTALL_FAILED_DUPLICATE_PERMISSION: … already owned by app.simple.inure[.play]`.

### Future feature commits

Append small, surgical commits on top of commit 1 so rebases stay trivial; `namespace` stays unchanged in all of them. Document notable ones here as the stack grows (mirroring how the appmanager / futokxkb skills list their feature commits).

## Versioning (local counter + `-P` injection — no commit needed)

The build.gradle edit above reads the version from `-P` properties, so **no per-build commit is needed**:

- **Base tag** = the tracked Inure release tag, e.g. `build107.0.2` (= the tag `custom` is rebased onto; `git describe --tags --abbrev=0` on `custom`). This is what **keys the counter**.
- **Display name** = the base tag with the leading `build` stripped (`${base_tag#build}`) → `107.0.2`. Cosmetic; used only for the versionName and the APK filename. (Stripping the prefix does **not** reset the counter, because the counter is keyed on the full tag, not the display name.)
- **Base code** = upstream's own versionCode for that tag, read with `git show <tag>:app/build.gradle` (e.g. `build107.0.2` → `10702`). Inure's codes are 5 digits derived from the build number.
- **N** = per-build iteration, from a local counter file `~/tmp/.shiroikuma_inure_build` (one line: `<base_tag> <N>`). Increment on each **successful** build; **reset to 1 when the base tag changes** (new upstream tag). Consumed only on success.
- **versionName** = `<display_name>+<N>` → `107.0.2+1`, `107.0.2+2`, … (`+` is legal in Android versionName and on ext4/FAT/`adb push`/GitHub assets — leave it unescaped).
- **versionCode** = `<base_code> * 10000 + N` → `107020001`, `107020002`, … This is far above the official app's code (`10702`) so Android always sees our build as newer, with 9999 builds of headroom per base; rebasing onto a newer tag raises the base so we stay ahead.
- Inject at the gradlew call: `-PshiroikumaVersionName="$our_name" -PshiroikumaVersionCode="$our_code"`.

## Signing

The `github` flavor sets `signingConfig = signingConfigs.release` (when a keystore is found), and `signingConfigs.release` reads `local.properties`: it looks for `~/work/_temp/keystore/key.jks` first (CI only; absent here) then falls back to `KEYSTORE_PATH`, with passwords from env-or-`local.properties`. So AGP produces an **already-signed, zipaligned** release APK — no separate `apksigner`/`zipalign` step. We use a dedicated keystore so the user owns the key. **`local.properties` is regenerated by the build pipeline on every run** (it is gitignored; never commit it) so it survives `git checkout`/`reset` and a fresh clone.

## NDK note

Upstream pins `ndkVersion = '29.0.13599879 rc2'` — an unreleased release candidate that is **not installable via sdkmanager** and is not on this machine. The installed NDK is `29.0.14206865`. Commit 1 made `ndkVersion` overridable, so always build with `-PshiroikumaNdk='29.0.14206865'`. If a future upstream bumps to a released NDK that is installed, the `-P` can be dropped (the committed default will just work). If the build fails on the NDK, check `ls ~/android-sdk/ndk/` and point `-PshiroikumaNdk` at whatever 29.x (or later) is present.

## One-time setup (already done — recorded for reproducibility)

Done during fork setup; listed so a fresh clone can be reproduced:

```bash
cd ~/git && git clone git@github.com:ShiroiKuma0/shiroikuma-inure.git
cd shiroikuma-inure
git remote add upstream https://github.com/Hamza417/Inure.git
git fetch upstream --tags
# dedicated keystore (idempotent)
mkdir -p ~/.android-keystores
[ -f ~/.android-keystores/inure.jks ] || keytool -genkeypair -v \
  -keystore ~/.android-keystores/inure.jks -alias inure -keyalg RSA -keysize 2048 \
  -validity 36500 -storepass inure123 -keypass inure123 \
  -dname "CN=shiroikuma inure, O=shiroikuma, C=JP"
# custom branch off the latest release tag + commit 1 (identity)
git checkout -b custom build107.0.2
# (apply the commit-1 edits above) then:
git add app/build.gradle app/src/main/res/values/non_translatable_string.xml
git commit -m "Customize for shiroikuma side-by-side install"
```

## Per-change workflow (the user tests between every change)

When making a NEW change to the fork:

1. **Make the edit on `custom`**, keep it small and surgical, then build (pipeline below) and let the user test on-device. Do not commit yet.
2. **Iterate** with more edits + rebuilds until the user is happy. Uncommitted edits stack on the working tree.
3. **Only when the user explicitly says to commit/push** do you `git commit` (a small focused commit on `custom`) and, separately, `git push` to `origin`. **Never push to GitHub on your own initiative.** A new feature commit appends on top of commit 1 and survives rebases.

## Build + sign + deploy pipeline

Run directly with the Bash tool. First build pulls a large dependency set and compiles the native CMake terminal lib (10–20+ min); later builds use the Gradle + configuration cache. There are no submodules to init.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$HOME/android-sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
cd ~/git/shiroikuma-inure
git checkout custom

# local.properties — regenerated each build; gitignored; never committed
cat > local.properties <<EOF
sdk.dir=$HOME/android-sdk
KEYSTORE_PATH=$HOME/.android-keystores/inure.jks
SIGNING_STORE_PASSWORD=inure123
SIGNING_KEY_ALIAS=inure
SIGNING_KEY_PASSWORD=inure123
EOF

# version: base tag = nearest tag on custom (keys the counter); display name = tag minus the
# "build" prefix; base code = that tag's upstream versionCode; N from the local counter (resets
# only when the base TAG changes, not when the prefix is stripped)
base_tag=$(git describe --tags --abbrev=0)
disp_name="${base_tag#build}"                     # build107.0.2 -> 107.0.2
base_code=$(git show "$base_tag:app/build.gradle" | grep -oE 'versionCode[[:space:]]+[0-9]+' | grep -oE '[0-9]+$')
counter="$HOME/tmp/.shiroikuma_inure_build"
stored_name=""; stored_n=0
[ -f "$counter" ] && read stored_name stored_n < "$counter"
if [ "$stored_name" = "$base_tag" ]; then N=$((stored_n + 1)); else N=1; fi
our_name="${disp_name}+${N}"                       # 107.0.2+2
our_code=$(( base_code * 10000 + N ))
apk_name="shiroikuma-inure_${our_name}_arm64-v8a.apk"
echo "Will produce: $apk_name (versionCode $our_code)"

# build github/release/arm64, signed by AGP.
# NOTE: invoke via `sh ./gradlew` — upstream commits gradlew as mode 644 (non-executable), so a bare
# `./gradlew` fails with "Permission denied" (rc 126). `sh ./gradlew` needs no +x and leaves the file
# mode untouched (a chmod +x would show as a spurious tracked change / rebase noise). Do NOT chmod it.
build_ok=0
sh ./gradlew :app:assembleGithubRelease \
  -PshiroikumaVersionName="$our_name" \
  -PshiroikumaVersionCode="$our_code" \
  -PshiroikumaNdk='29.0.14206865' \
  --console=plain && build_ok=1

if [ "$build_ok" = 1 ]; then
  echo "$base_tag $N" > "$counter"             # consume the build number only on success (keyed on the tag)
  built=$(ls -t app/build/outputs/apk/github/release/*.apk | head -1)
  mkdir -p ~/tmp && cp "$built" ~/tmp/"$apk_name"   # local backup, unconditional
  ls -lh ~/tmp/"$apk_name"
else
  echo "BUILD FAILED — no APK. Diagnose the 'What went wrong' / 'Caused by' lines; do NOT push any leftover APK."
fi
```

- If the build fails on the NDK (`Failed to find NDK … 29.0.13599879`), confirm `-PshiroikumaNdk` points at an installed NDK (`ls ~/android-sdk/ndk/`).
- If it fails on signing being skipped (unsigned APK), `local.properties` wasn't written or `KEYSTORE_PATH` is wrong — the APK won't install. Regenerate it.
- `./gradlew: Permission denied` (rc 126): gradlew is committed mode 644 — use `sh ./gradlew` (as above), don't `chmod +x` (that pollutes the tree).
- A stale Gradle daemon on the wrong JVM: `sh ./gradlew --stop` then rebuild.

## adb push (the standing rule — ask, then wait for "Push")

On a successful build the APK is already in `~/tmp/`. **Ask via the AskUserQuestion UI** whether to push it to the phone — every build, never auto-push. Only when the user says to push ("Push"):

```bash
adb devices
adb shell mkdir -p /sdcard/tmp
adb push ~/tmp/"$apk_name" /sdcard/tmp/"$apk_name"
adb shell ls -l /sdcard/tmp/"$apk_name"   # confirm the size matches ~/tmp
```

The user installs from `/sdcard/tmp/` via the on-device file manager. **Never `adb install` / `adb uninstall`.** If the cable is absent, the `~/tmp/` copy is the fallback (KDE Connect / Bluetooth).

## Related skills

- **`upstream-new-version`** — check whether Hamza417/Inure has a newer release tag and, if so, fast-forward `master`, rebase `custom` onto the new tag (reconciling small conflicts, stopping to plan on significant ones), and rebuild via this skill.
