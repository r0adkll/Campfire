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
./gradlew createModuleGraph

# Store screenshots (phone | seven | ten) → fastlane/metadata/android (see tools/screenshots/README.md)
tools/screenshots/run.py --class phone

# End-to-end testing against a throwaway local ABS server + emulator (see tools/testbed/README.md;
# the campfire-device-tester agent drives it). Never test against a real server. Screenshots and
# screen recordings (`record start|stop`) land in the run's captures dir, printed by `status`/`down`.
tools/testbed/testbed.py up

# Guided release of `campfire.version` (gradle.properties): baseline profiles (local GMD or
# emulator.wtf — token from Keychain via `scripts/release set-ew-token`), CHANGELOG.md roll,
# fastlane changelog (over the 500-char store limit, `claude -p` condenses it in the background
# while baselines run; review/redo/edit the draft, or trim in $EDITOR), GitHub release. Gradle
# output goes to build/release/*.log. `--skip-baseline`, `--emulator-wtf`, `--yes`, `--dry-run`,
# `--no-claude`, `--notes-model <model>` (default opus).
scripts/release
```

## Technology Stack

- **Language**: Kotlin 2.4.10, Compose Multiplatform 1.11.1
- **Presentation**: Slack's Circuit (state machine-driven UI)
- **Networking**: Ktor Client with OIDC auth
- **Database**: SQLDelight (multiplatform SQLite) + Store5 (cache layer)
- **DI**: [Metro](https://zacsweers.github.io/metro/) (compiler plugin, compile-time, annotation-based; also generates the Circuit factories)
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
  private val screen: MyScreen,
  private val navigator: Navigator,
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

### DI (Metro)

- Every module that declares or contributes bindings applies `id("app.campfire.di")` (the `app.campfire.ui` convention applies it for UI modules). There is no KSP; contributions in any sourceset (commonMain or platform) just work.
- Graphs: `AndroidAppComponent` / `DesktopApplicationComponent` / `IosApplicationComponent` are the `@DependencyGraph(AppScope::class)` roots, one per platform so platform contributions are visible. `UserComponent` (UserScope) and the per-platform UiScope components are `@GraphExtension`s whose factories are `@ContributesTo(AppScope::class)`.
- Circuit: `@CircuitInject` presenters are plain `@Inject` classes — the screen and `Navigator` are ordinary constructor params (no `@Assisted`); UI functions may take injected params alongside state/modifier. Default parameter values are not honored for Circuit-injected params, so optional features bind a fallback that the optional module `replaces` (see `UnsupportedAiThemeBuilder`).
- Other assisted injection uses `@AssistedInject` plus an `@AssistedFactory fun interface` (e.g. `SearchPresenterFactory`). Metro does not treat typealiases as distinct binding keys and treats `() -> T` as a provider, so never bind a function-type typealias — use a `fun interface` (e.g. `CampfireAppBar`) or an `@Inject` class.
- `@ContributesBinding`/`@ContributesIntoSet` take `binding = binding<Type>()` when the class has several supertypes; `@Provides` functions in one interface need distinct names.
- A platform sourceset can override a common binding with `@ContributesBinding(Scope::class, replaces = [CommonImpl::class])`. Precedents: `AndroidDiscoverScanTracker` replaces `InProcessDiscoverScanTracker` (same module), `MediaRouterCastController` replaces `NoOpCastController` (cross-module).
- Confirm which implementation a graph constructs with Metro's reports: `./gradlew :app:android:compileStandardDebugKotlin -Pcampfire.metro.reports=true --rerun`, then look under `app/android/build/reports/metro/`.
- Framework-constructed objects (Services, WorkManager workers) resolve dependencies through `ComponentHolder` accessor interfaces (`@ContributesTo` on the scope), re-resolved fresh on each use — the `UserComponent` is rebuilt on every session change (see `AudioPlayerService`, `DiscoverScanWorker`).

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

The changelog describes what the user gets in the release, not the PR history: a feature shipped across multiple (often stacked) PRs keeps ONE entry covering the whole feature — the first PR adds the line, later PRs refine it (or leave it alone) rather than adding entries of their own.

Each platform only sees the entries that apply to it: the in-app What's New filters by the running platform, and `scripts/release` writes the Play / F-Droid notes from the Android entries. Tag an entry that applies to some platforms only with a leading list — `- [Desktop] …`, `- [Android, iOS] …` (valid tags: `Android`, `Desktop`, `iOS`; an unknown tag fails the build). Leave shared changes untagged; they apply to every platform. Because the tag already says where the change applies, don't also name the platform in the text: write `- [Desktop] Download audiobooks for offline listening`, not `- [Desktop] Desktop can download…`. When a platform ships its first release, set its `firstRelease` in `ChangelogPlatform` (`gradle/build-logic`) so older untagged entries stay out of its What's New.

## Database Migrations

Schema lives in `data/db/core/src/commonMain/sqldelight/app/campfire/data/*.sq`; migrations live alongside in `migrations/{N}.sqm` and the baseline schema dump is `app/campfire/databases/1.db`. SQLDelight's `verifyCommonMainCampfireDatabaseMigration` task compares the migrated database against the fresh `.sq` schema — any drift fails the build.

Danger fails any PR touching a `.sq` file that doesn't also touch a `.sqm`, unless the PR carries the `ignore-db-change` label. When a PR touches `.sq` files, pick the branch that matches:

- **Query-only change** (added/edited statements, table shapes untouched): no migration needed — add the `ignore-db-change` label to the PR.
- **Schema change in a database without migrations** (e.g. `BookInfoDatabase`, which versions by file name and rebuilds destructively): add the `ignore-db-change` label.
- **Schema change in the main Campfire database** — reflect it in a `.sqm` (which also satisfies Danger, no label):
  - **If the latest `.sqm` is newer than the most recent GitHub release/tag (i.e. not yet shipped)**: fold the change into that file. Users mid-upgrade only run unshipped migrations once. Check with `git log <latest-tag>..HEAD -- data/db/core/.../migrations/`.
  - **Otherwise**: create a new `{N+1}.sqm` with the additive change.

Run `./gradlew :data:db:core:verifyCommonMainCampfireDatabaseMigration` after any schema or migration edit to confirm parity. SQLDelight is strict about column ordinal positions — adding a column mid-table requires a full `CREATE TABLE ... _new` + copy + `DROP` + `RENAME`, not `ALTER TABLE ADD COLUMN`.

## Android Background Work & Notifications

- Long-running background jobs are regular unique WorkManager work with a guarded `setForeground` promotion (`DiscoverScanWorker` is the template: userId validation against the restored session, `NonCancellable` finally for terminal persistence, a `Scoped` multibinding cancelling the unique work on logout). Regular work, not expedited — on minSdk 31 expedited work runs as a job whose notification never shows. Declare the FGS type by merging it onto WorkManager's `SystemForegroundService` in the module's androidMain manifest (`tools:node="merge"`).
- Notification ids and channels are one app-wide ledger — claim the next free id and record it here: 100 playback (`app.campfire.notifications.playback`), 101 downloads (`app.campfire.notifications.download`), 102 scan progress + 103 scan completed (`app.campfire.notifications.scan`).
- A notification tap-through to a screen gets a release-safe deep link: a `campfire_`-prefixed extra in `DeepLinkKeys`, parsed in the non-automation branch of `Intent.toDeepLink`, handled in `LoggedIn.kt`. Give each PendingIntent its own request code — `Intent.filterEquals` ignores extras, so same-code PendingIntents to `MainActivity` clobber each other.

## Pre-commit Hook

Run `./gradlew bootstrap` to install the pre-push hook from `scripts/pre-push`.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for `r0adkll/Campfire`, operated via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root (created lazily by `/domain-modeling`). See `docs/agents/domain.md`.
