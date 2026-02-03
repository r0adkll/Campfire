# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Campfire is an unofficial Kotlin Multiplatform native client for [Audiobookshelf](https://www.audiobookshelf.org/) supporting Android, iOS, and Desktop platforms using Jetbrains Compose Multiplatform for UI.

## Essential Commands

```bash
# Code formatting (required before commits, CI auto-commits fixes on PRs)
./campfire -v spotless

# Run tests with coverage
./gradlew test allTests koverXmlReport

# Build platforms
./gradlew :app:android:assembleAlphaRelease     # Android APK
./gradlew :app:desktop:packageDistributionForCurrentOS  # Desktop
./gradlew iosSimulatorArm64Test                 # iOS tests

# Generate module dependency graph (updates docs/architecture/MODULARIZATION.md)
./gradlew moduleGraph
```

## Technology Stack

- **Language**: Kotlin 2.3.0, Compose Multiplatform 1.10.0
- **Presentation**: Slack's Circuit (state machine-driven UI)
- **Networking**: Ktor Client with OIDC auth
- **Database**: SQLDelight (multiplatform SQLite) + Store5 (cache layer)
- **DI**: kotlin-inject + Kimchi (compile-time, annotation-based)
- **Code Style**: ktlint via Spotless

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

## Pre-commit Hook

Run `./gradlew bootstrap` to install the pre-push hook from `scripts/pre-push`.
