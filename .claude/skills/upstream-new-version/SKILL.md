---
name: upstream-new-version
description: Check whether Inure App Manager upstream (Hamza417/Inure) has a newer release tag and, if so, sync the shiroikuma.inure fork to it — fast-forward `master`, rebase the `custom` commit stack onto the new tag (reconciling conflicts when they're small, or stopping to plan with the user when they're significant), then build the new signed APK per the inure-build skill. Use this skill whenever the user runs /upstream-new-version, or asks to "check for a new Inure version", "sync to the latest Inure release", "update to the new upstream", "rebase onto the new tag", or otherwise wants the Inure fork brought up to a newer upstream release. This is the orchestration layer on top of inure-build; read that skill for all the build/rebase/version detail this one references.
---

# Sync the Inure fork to a new upstream release

One-command upstream sync for 白い熊's `shiroikuma.inure` fork: **check → (if new) rebase → build → test → push**. This is the **orchestration layer**; every concrete fact (remotes, keystore, version scheme, the build pipeline, the commit-1 anchor) lives in the **`inure-build`** skill — **read it before running this**, especially "Customization commits", "Versioning", and "Build + sign + deploy pipeline".

## The one discipline that overrides everything: don't push to GitHub until the user says so

The entire rebase + build happens on the **local** working tree as a scratchpad. **No `git push` — not `master`, not `custom` — until the user explicitly tells you to push to the remote.** A rebase rewrites local `custom` history and is freely re-runnable (`git rebase --abort`, or reset to `origin/custom`) right up until that point. Build and let the user test on-device first. (Delivery of the build to the phone is separate and **automatic** — `/after-build` pushes/scp's it without asking; only pushing to GitHub is held back.)

## Step 0 — Preconditions

- cwd is the repo (`~/git/shiroikuma-inure`); remotes are `origin` (SSH, fork) and `upstream` (HTTPS, Hamza417, fetch-only) — confirm with `git remote -v`.
- Working tree clean (`git status --short` empty). If dirty, surface it and ask before proceeding — uncommitted scratch work would be caught in the rebase. (`local.properties` is gitignored, so it never shows here.)
- Currently on (or able to check out) `custom`.

## Step 1 — Check upstream for a newer release tag

Inure tags releases as bare `buildNN[.x.y]` (e.g. `build107.0.2`, `build106.5.1`) — no `v` prefix, no `-rc`. The fork tracks the **latest release tag**, not in-development master (upstream's master often sits one untagged patch ahead, e.g. `build107.0.3` before that tag exists, plus F-Droid/UAD-list bot commits we don't want as our base).

```bash
cd ~/git/shiroikuma-inure
git fetch upstream --tags
git fetch origin                 # so origin/master / origin/custom are current for later

# base tag of the custom stack = the nearest release tag custom was rebased onto
base_tag=$(git describe --tags --abbrev=0 custom)

# latest upstream release tag (version-sorted; the constant "build" prefix sorts fine under sort -V)
latest_tag=$(git tag -l | grep -E '^build[0-9]' | sort -V | tail -1)

echo "custom is based on:        $base_tag"
echo "latest upstream tag:       $latest_tag"
```

- **`latest_tag == base_tag`** → already on the newest upstream release tag. Report it ("custom is on `build107.0.2`, the newest upstream tag — nothing to sync") and **stop**.
- **`latest_tag != base_tag`** (it will be strictly newer, since `sort -V | tail -1` is the max over a pool that includes `base_tag`) → continue to Step 2. First show the user what's coming: `git log --oneline "$base_tag".."$latest_tag"` and the replay count `git rev-list --count "$base_tag"..custom`.

`git describe --tags --abbrev=0 custom` names the nearest tag because `custom` is rebased onto a release tag on upstream's first-parent history. Cross-check against `~/tmp/.shiroikuma_inure_build`'s first field if it exists.

## Step 2 — Fast-forward `master` (local only; push deferred)

`master` is a pure mirror of upstream, fast-forward only, never carries our changes.

```bash
git checkout master
git merge --ff-only upstream/master
git checkout custom
```

Do **not** `git push origin master` here — defer it to the push step.

## Step 3 — Rebase the `custom` stack onto the new tag

```bash
git rebase "$latest_tag"
```

Then triage the outcome.

### Clean rebase → continue to Step 4.

### The one expected, recurring conflict: the version lines in `app/build.gradle`

Commit 1 (`Customize for shiroikuma side-by-side install`) **replaced** upstream's literal `versionCode <N>` / `versionName "buildNN…"` lines with our `-P`-driven block. Upstream bumps those literals every release, so replaying commit 1 onto a new tag conflicts on exactly those two lines **almost every time**. This is **"not huge"** — resolve in place:

- **Keep OUR `-P` block** (the `versionCode((project.findProperty('shiroikumaVersionCode') …` + `versionName(project.findProperty('shiroikumaVersionName') …` lines and their comment); **discard upstream's new literal `versionCode`/`versionName` lines.** The stale `build107.0.2` example inside our fallback comment does not need updating — the pipeline always passes `-P`, so the fallback is never used.
- Keep our `applicationId "shiroikuma.inure"`, the `ndk { abiFilters 'arm64-v8a' }` block, and the overridable `ndkVersion` line; take upstream's surrounding changes.
- The `app_name` edit in `non_translatable_string.xml` only conflicts if upstream edits near it (rare) → keep `白い熊 Inure`.

Then `git add app/build.gradle` (and the strings file if touched) and `git rebase --continue`.

### Other conflicts → decide: "not huge" vs "significant"

**"Not huge" — resolve in place, `git add`, `git rebase --continue`:** the version-line conflict above; pure context-line shifts where our hunk obviously slots into moved-but-equivalent code; a commit going **empty** because upstream did the same thing (let git skip it); a small handful (≈1–3) of mechanical conflicts in feature commits where our change clearly maps onto the new code.

**"Significant" — STOP, do not guess, plan with the user:**
- Upstream **refactored / moved / renamed** something a feature commit depends on so the patch no longer maps cleanly.
- Many commits conflict, or the **same file conflicts repeatedly** across several replayed commits.
- A **semantic** conflict: hunks merge textually but an API/behaviour upstream changed, so you can't be confident the feature still works without analysis.
- Any conflict whose correct resolution isn't obvious from the inure-build documentation.

**When significant**, don't `--abort` silently and don't force a resolution:
1. Gather the picture without changing anything: `git status` (which commit is replaying), the conflicted hunks (`git diff`), and what upstream changed (`git log --oneline "$base_tag".."$latest_tag" -- <file>` / `git show`).
2. Identify **which of our commits** is conflicting and **why**.
3. **Present a plan and ask the user how to proceed** (AskUserQuestion, or EnterPlanMode for a multi-commit mess). Typical options: resolve together; re-derive the affected commit from inure-build; drop/defer the conflicting commit; or abort the whole sync (`git rebase --abort`, which returns the tree to exactly where it was — nothing lost). Make clear aborting is safe and re-runnable.

Don't push through a significant rebase just to "get it building" — a silently mis-resolved feature is worse than a paused sync.

## Step 4 — Build the new APK (apply the inure-build pipeline)

Build directly with Bash per **inure-build**'s "Build + sign + deploy pipeline". The version counter **resets to N=1** automatically because the base tag changed → versionName `<new-tag with the "build" prefix stripped>+1` (e.g. a new `build107.0.3` tag → `107.0.3+1`). There are no submodules to init.

- If the build **fails on the rebase result** (a compile error in code our commits touch), treat it like a significant conflict: diagnose, and if it stems from the rebase, replan with the user rather than patching blindly.
- Toolchain reminders: JDK 21, SDK platform-36 / build-tools 36.1.0, and `-PshiroikumaNdk='29.0.14206865'` (upstream's pinned RC NDK is not installable). `./gradlew --stop` if a stale daemon picked the wrong JVM.

## Step 5 — User tests on-device

On a successful build, deliver it **automatically** via the global **`/after-build`** skill (the inure-build "Deliver the build" rule) — no transfer prompt: it runs `/adb-check` UNSANDBOXED then `/adb-push` to `/sdcard/tmp/` if the phone is connected, else `/scp` to `skhw`, announcing what landed. Then **wait** — the user installs over the previous fork build (same signing key → in-place update) and verifies the customizations still work on the new base. They may report regressions from the upstream bump; iterate locally (more edits, rebuild) — still no push to GitHub.

## Step 6 — Only when the user says to push to GitHub

Then, in one flow:

```bash
git push origin master                      # the deferred ff from Step 2
git push --force-with-lease origin custom    # the rebased stack (history rewritten)
```

`--force-with-lease` (never bare `--force`) so a surprise update to `origin/custom` aborts the push instead of clobbering it.

Then **update the docs to the new base**: the version examples and the `build107.0.2` references in `inure-build`'s *Project identity* / *Versioning* / *Customization commits*, and the base-tag reference in this skill's Step 1. Commit those doc updates on `custom` (plain subject, no prefix) and push (force-with-lease). Treat the sync as incomplete until the docs reflect the new base.

## Reference — conflict watch-points (condensed from inure-build)

- **Commit 1** (`Customize for shiroikuma side-by-side install`): `app/build.gradle` `defaultConfig` — `applicationId "shiroikuma.inure"`, the `-P`-driven `versionCode`/`versionName` block (conflicts on upstream's literal version bump every release → keep ours), the `ndk { abiFilters 'arm64-v8a' }` block; and the overridable `ndkVersion` line; plus `non_translatable_string.xml` `app_name` → `白い熊 Inure`. Leave `namespace`, the `play` flavor suffix, and `resValue "string", "versionName", versionName` untouched.
- **Commit 3** (`Namespace terminal permissions per applicationId`): the `${applicationId}.terminal.permission.*` names in **both** `app/src/main/AndroidManifest.xml` and `app/src/github/AndroidManifest.xml`, plus the `BuildConfig.APPLICATION_ID`-derived constants in `Term.java`. Conflicts only if upstream edits those manifest lines or the `Term.java` constants; keep ours in both manifests (a missed one re-introduces `INSTALL_FAILED_DUPLICATE_PERMISSION`). Required for side-by-side install.
- **General rule:** if conflicts feel non-trivial, re-derive commit 1 from inure-build rather than fighting the merge; for any future feature commit, take the "significant → plan with the user" path.

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
