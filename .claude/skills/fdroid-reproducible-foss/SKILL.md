---
name: fdroid-reproducible-foss
description: Rules and reliable local tests for keeping Campfire's `foss` flavor F-Droid-compatible — a clean source scan (using scandelete, never scanignore) and a byte-for-byte reproducible build. Trigger when editing the fdroiddata metadata; adding, removing, or moving a proprietary dependency / Gradle plugin / custom Maven repository; changing anything the foss flavor builds (app/android, gradle/build-logic, settings.gradle.kts, baseline profiles, R8/ProGuard); cutting a release F-Droid mirrors; or when asked to "check the F-Droid scan", "verify reproducibility", or "why did F-Droid flag X".
---

## What F-Droid requires of the `foss` flavor

Two independent hard requirements. Both must hold or the fdroiddata pipeline goes red:

1. **A clean source scan** — the checked-out source must contain no "unknown maven repos" and no proprietary "usual suspects" once `scandelete` has run. Maintainers **disallow `scanignore`** (F-Droid docs: it is only "where there is a very good reason"); the sanctioned mechanism is **`scandelete`**, which deletes files not needed by the build before scanning.
2. **A byte-for-byte reproducible build** — F-Droid rebuilds `assembleFossRelease` from the release tag and it must match our published `campfire-foss-release.apk` exactly, so F-Droid ships our-signed APK (`AllowedAPKSigningKeys`).

The MR: `https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46619` (metadata `metadata/app.campfire.android.yml`).

## Invariants you must not break

- **The foss flavor never depends on a proprietary module.** Firebase, Cast, ML Kit, Mixpanel, Play in-app-updates, App Distribution are wired to `standard*` / `alpha*` configurations only — **never `foss*`** (see `app/android/build.gradle.kts`). If you add a proprietary integration, follow the same pattern, and put its build files where F-Droid can `scandelete` them (see below).
- **No custom Maven repository literal in any scanned `.gradle`/`.gradle.kts`.** The scanner flags **any** `maven(...)` / `maven { url … }` call whose URL isn't on F-Droid's allow-list — and it captures whatever is inside the parens, so even `maven(someVariable)` is flagged as `unknown maven repo 'someVariable)'`. Reading the URL from a property does **not** help. Custom repos must live in a `scandelete`-able standalone script (see the emulator.wtf pattern).
- **Reproducibility hooks stay in place:**
  - `Project.normalizeFossReleasePgMapId()` (in `gradle/build-logic/convention/.../Reproducible.kt`), called from `app/android/build.gradle.kts` — zeroes R8's `pg-map-id` in the foss DEX (the one thing that varied per build environment).
  - `baselineProfile { variants { create("foss") { dexLayoutOptimization = false } } }` in `app/android/build.gradle.kts` — keeps the baseline profile but drops the non-deterministic startup DEX layout for foss only.
- **`campfire.version` / `campfire.versionCode` stay in lockstep** (`MMmmppRR`, asserted by `verifyVersionCode` + `scripts/release`). F-Droid's `UpdateCheckData` reads `campfire.versionCode` from `gradle.properties` on the tag.

## How the scanner actually works (so you can reason about it)

Source: `fdroidserver/scanner.py`, `scan_source()`. F-Droid CI runs **fdroidserver `master`** (installed from the tarball on `debian:trixie-slim`, not pip), so test against master.

- **Only these file types get content-scanned:** `.java` (DexClassLoader only), `.gradle` / `.gradle.kts` (usual-suspects + maven-URL), and binaries (`.apk/.jar/.aar/.class/.dex/.so/.zip/.gz/.wasm`, extensionless/`.bin/.out/.exe`, executables). **`gradle.properties`, `.kt`, `.py`, and shell scripts are NOT scanned** for repos or suspects — confirmed by reading the dispatch and by A/B test.
- **Usual suspects** resolve `libs.*` version-catalog accessors through `libs.versions.toml`, so `libs.firebase.crashlytics.gradlePlugin` in a `.gradle.kts` is flagged even though the coordinate is in the TOML. `//` comments are ignored; only real `implementation(...)`-style lines match.
- **`scandelete` deletes listed paths before the scan runs**, so a flagged file that the foss build doesn't need is removed and never scanned. This is why the isolated files below can each carry the flagged content.

## Pattern: isolating a proprietary Gradle plugin so it can be scandelete'd

Firebase's plugins can't sit in `build.gradle.kts` (root) or `gradle/build-logic/convention/build.gradle.kts` — those are scanned and can't be deleted. So:

- The proprietary plugin deps + the convention plugin live in a **separate build-logic module** `gradle/build-logic/firebase/` (its `build.gradle.kts` carries `libs.firebase.*.gradlePlugin`).
- `gradle/build-logic/settings.gradle.kts` includes it only `if (file("firebase/build.gradle.kts").exists())`.
- `convention/build.gradle.kts` does `runtimeOnly(project(":firebase"))` under the same `if (file(...).exists())` guard.
- The plugin is applied from a convention plugin (`AndroidApplicationConventionPlugin`) via `pluginManager.apply("app.campfire.firebase")`, guarded by `rootProject.file("app/android/google-services.json").exists()` (absent in F-Droid).
- fdroiddata `scandelete`s `gradle/build-logic/firebase/build.gradle.kts` → module not included, plugin never applied, build-logic still compiles.

**Gotcha:** legacy `apply(plugin = "id")` does NOT resolve an included-build plugin — it must be applied from inside a convention plugin (on the build-logic classpath) or via the `plugins {}` block.

## Pattern: a custom Maven repo needed only for tooling (emulator.wtf)

The emulator.wtf Gradle plugin resolves from **mavenCentral**; only its `ew-cli` runner is exclusive to `maven.emulator.wtf`, and only when actually running on emulator.wtf (CI baseline generation) — never a foss/normal build. So:

- The repo is declared in a standalone **`gradle/emulatorwtf-repo.gradle.kts`** (a settings script adding it via `dependencyResolutionManagement`).
- `settings.gradle.kts` applies it only `if (file("gradle/emulatorwtf-repo.gradle.kts").exists())` — the apply line has no maven call, so it isn't flagged.
- `app/baselineprofile/build.gradle.kts` sets `emulatorwtf { repositoryCheckEnabled.set(rootProject.file("gradle/emulatorwtf-repo.gradle.kts").exists()) }` (extension name is lowercase `emulatorwtf`).
- fdroiddata `scandelete`s `gradle/emulatorwtf-repo.gradle.kts` → deleted before scan (0 errors), apply skipped, repo check off. Any real emulator.wtf use just needs the file present — no extra flags.

Use this same shape for any future third-party repo that only serves build/test tooling.

## Testing — do this before pushing metadata or a release

Commit your changes first (the scanner test archives a git ref). Then:

### 1. Run the real F-Droid scanner
```bash
.claude/skills/fdroid-reproducible-foss/scan-source.sh            # scans HEAD
.claude/skills/fdroid-reproducible-foss/scan-source.sh <git-ref>  # scans a ref
```
It exports the ref, applies the same `scandelete` list the metadata declares, runs fdroidserver `master` `scan_source()` in Docker, and prints `SCAN COUNT` + each error. **Must be 0.** Keep the script's `SCANDELETE` list identical to the metadata's `scandelete:` block.

### 2. Confirm the foss build still configures with those files deleted (the F-Droid scenario)
```bash
git worktree add --detach /tmp/fdsim <git-ref>
cd /tmp/fdsim && rm <the scandelete files>          # + ensure no app/android/google-services.json
./gradlew :app:android:assembleFossRelease --dry-run --console=plain   # expect BUILD SUCCESSFUL
cd - && git worktree remove --force /tmp/fdsim
```
(Use a **worktree**, not `git archive | tar` — the latter drops the `gradlew` exec bit and munges permissions.)

### 3. Verify reproducibility of a published foss APK
The pg-map-id in the published `campfire-foss-release.apk` must be all-zeros:
```bash
unzip -p campfire-foss-release.apk classes.dex | grep -a -o '"pg-map-id":"[0-9a-f]*"' | head
# expect "pg-map-id":"0000…0000"
```
After F-Droid's pipeline runs, the `fdroid build` job log should say *"compared built binary to supplied reference binary successfully"*. If it differs, pull the job log and `diffoscope` our APK against F-Droid's `…_<versionCode>.binary.apk` artifact to find the delta.

## Updating the fdroiddata metadata

- Keep the `scandelete:` list in sync with `scan-source.sh` (currently 5 files; see `scandelete-metadata-companion.yml` in the session scratchpad for the canonical block).
- Bump `versionName` / `versionCode` / `commit` to the release tag; `AllowedAPKSigningKeys` stays `4a3be90f…`; keep `Binaries`, `AutoUpdateMode: Version`, `UpdateCheckMode: Tags`, `UpdateCheckData`.
- **Canonicalize with the CI-exact fdroidserver** (debian:trixie-slim + master tarball) before pushing, or `rewritemeta`/`lint` will reformat differently than your local pip install:
  ```bash
  docker run --rm -v "$PWD":/repo -w /repo debian:trixie-slim bash -c '
    apt-get update -qq && apt-get install -qy --no-install-recommends fdroidserver curl ca-certificates git python3-yaml
    mkdir /fds && curl -s https://gitlab.com/fdroid/fdroidserver/-/archive/master/fdroidserver-master.tar.gz | tar -xz -C /fds --strip-components=1
    export PATH=/fds:$PATH PYTHONPATH=/fds:/fds/examples
    fdroid rewritemeta app.campfire.android && fdroid lint app.campfire.android'
  ```
- Commit as the user only — **no `Co-Authored-By` trailer** on fdroiddata commits (they must appear to come from the user's account alone). Commit signing uses 1Password SSH; if it errors with `failed to fill whole buffer`, ask the user to unlock 1Password and retry.

## Gotchas learned the hard way

- `handleproblem` in the scanner is **silent when `common.get_options()` is None** (no log, no JSON) but still increments the count. To surface which file/message counts, pass `json_per_build=scanner.MessageStore()` and set `common.options` (see `scan-source.sh`).
- The two `central.sonatype.com` snapshot repos in `settings.gradle.kts` are gated behind `campfire.config.enableSnapshots` and are on F-Droid's allow-list, so they don't count.
- Comments that mention a coordinate or hostname are fine — the scanner ignores `//` lines and only matches real dependency/`maven(...)` calls.
- Baseline profiles matter to end users; never "fix" reproducibility by dropping the profile. Drop only `dexLayoutOptimization` for foss.
