# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Campfire is an unofficial Kotlin Multiplatform native client for [Audiobookshelf](https://www.audiobookshelf.org/) supporting Android, iOS, and Desktop platforms using Jetbrains Compose Multiplatform for UI.

## Essential Commands

```bash
# Code formatting (required before commits, CI auto-commits fixes on PRs)
./scripts/ktlint --format        # apply fixes
./scripts/ktlint --check         # verify only

# Run tests with coverage
./gradlew test allTests koverXmlReport

# Build platforms
./gradlew :app:android:assembleAlphaRelease     # Android APK
./gradlew :app:desktop:packageDistributionForCurrentOS  # Desktop
./gradlew iosSimulatorArm64Test                 # iOS tests

# Generate module dependency graph (updates docs/architecture/MODULARIZATION.md)
./gradlew moduleGraph

# Store screenshots (phone | seven | ten) → fastlane/metadata/android (see tools/screenshots/README.md)
tools/screenshots/run.py --class phone

# Guided release of `campfire.version` (gradle.properties): baseline profiles (local GMD or
# emulator.wtf — token from Keychain via `scripts/release set-ew-token`), CHANGELOG.md roll,
# fastlane changelog (trims over-limit text in $EDITOR), GitHub release. Gradle output goes
# to build/release/*.log. `--skip-baseline`, `--emulator-wtf`, `--yes`, `--dry-run`.
scripts/release
```

## Technology Stack

- **Language**: Kotlin 2.4.10, Compose Multiplatform 1.11.1
- **Presentation**: Slack's Circuit (state machine-driven UI)
- **Networking**: Ktor Client with OIDC auth
- **Database**: SQLDelight (multiplatform SQLite) + Store5 (cache layer)
- **DI**: kotlin-inject + Kimchi (compile-time, annotation-based)
- **Code Style**: ktlint (invoked via `scripts/ktlint`)

## Architecture

### Module Patterns

**Standalone modules** - Self-contained, single purpose:
- `:core` - Domain models, DI scopes, common utilities
- `:ui:appbar`, `:infra:debug`, `:infra:shake`

**Grouped modules** - Feature-driven with api/impl/ui pattern:
```
:features:{name}/
├── api/   # Lightweight interface (depends only on :core and other :api modules)
├── impl/  # Implementation with data layer access
├── ui/    # Circuit screens and presenters
└── test/  # (optional) Fakes and test utilities
```

### Key Directories

- `/features/` - Feature modules (auth, libraries, sessions, home, search, settings, stats, series, collections, author, user, filters)
- `/data/` - Data layer (network, db, account, analytics, crashreporting)
- `/infra/` - Infrastructure (audioplayer, shake, debug, updates)
- `/ui/` - Shared UI (theming, widgets, navigation, appbar, drawer)
- `/core/` - Central domain models, DI scopes, utilities
- `/gradle/build-logic/convention/` - Build convention plugins

### Circuit UI Pattern

Each screen follows this structure (use "Circuit Screen" file template in IDE):

```kotlin
// Screen key - navigation & data carrier
@Parcelize
data class MyScreen(val id: String) : Screen

// State and events
@Immutable
data class MyUiState(
  val data: LoadState<MyData>,
  val eventSink: (MyEvent) -> Unit,
) : CircuitUiState

sealed interface MyEvent {
  data object Back : MyEvent
}

// Presenter - drives state via Compose
@CircuitInject(MyScreen::class, UserScope::class)
@Inject
class MyPresenter(
  @Assisted private val screen: MyScreen,
  @Assisted private val navigator: Navigator,
  private val repository: MyRepository,
) : Presenter<MyUiState> {
  @Composable
  override fun present(): MyUiState { /* ... */ }
}

// UI - pure composable
@CircuitInject(MyScreen::class, UserScope::class)
@Composable
fun MyUi(state: MyUiState, modifier: Modifier = Modifier) { /* ... */ }
```

### DI Scopes

- `AppScope` - App-level singletons (APIs, database)
- `UserScope` - Per-user instances (created on login, destroyed on logout)

## Code Coverage Requirements

- **Minimum overall**: 50% line coverage
- **Minimum changed files**: 80% line coverage
- Add `skip-coverage` label to PR to bypass

## Key Conventions

- `:api` modules can only depend on `:core` or other `:api` modules
- `:impl` modules provide DI bindings with appropriate scope
- Use `@CircuitInject` for screens with the appropriate scope (usually `UserScope`)
- `@Parcelize` is multiplatform via expect/actual (see `ParcelizeConventionPlugin`)
- Package structure: `app.campfire.[module].[submodule]`
- Do not add comments to dependency notations in `build.gradle.kts` files or in `gradle/libs.versions.toml` — keep dependency declarations bare. If a dependency exists for a non-obvious reason, explain it in the PR/commit message instead.
- Do not co-sign commits.

## Changelog

Every PR that changes app behavior must include an entry in `CHANGELOG.md` under `## [Unreleased]`, in the most appropriate [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) category (Added / Changed / Deprecated / Removed / Fixed / Other Notes & Contributions). Write a single concise, user-facing line describing the change — no implementation details (e.g. "Offline playback failing when the server is unreachable", not "Split the ExoPlayer SimpleCache into download and streaming caches"). Update the changelog before opening the PR.

## Database Migrations

Schema lives in `data/db/core/src/commonMain/sqldelight/app/campfire/data/*.sq`; migrations live alongside in `migrations/{N}.sqm` and the baseline schema dump is `app/campfire/databases/1.db`. SQLDelight's `verifyCommonMainCampfireDatabaseMigration` task compares the migrated database against the fresh `.sq` schema — any drift fails the build.

Whenever you change a `.sq` file, reflect the change in a `.sqm`:

- **If the latest `.sqm` is newer than the most recent GitHub release/tag (i.e. not yet shipped)**: fold the change into that file. Users mid-upgrade only run unshipped migrations once. Check with `git log <latest-tag>..HEAD -- data/db/core/.../migrations/`.
- **Otherwise**: create a new `{N+1}.sqm` with the additive change.

Run `./gradlew :data:db:core:verifyCommonMainCampfireDatabaseMigration` after any schema or migration edit to confirm parity. SQLDelight is strict about column ordinal positions — adding a column mid-table requires a full `CREATE TABLE ... _new` + copy + `DROP` + `RENAME`, not `ALTER TABLE ADD COLUMN`.

## Pre-commit Hook

Run `./gradlew bootstrap` to install the pre-push hook from `scripts/pre-push`.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for `r0adkll/Campfire`, operated via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root (created lazily by `/domain-modeling`). See `docs/agents/domain.md`.
