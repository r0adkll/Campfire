---
name: compose-modifier
description: Enforce the Compose guideline that every `@Composable` function must declare `modifier: Modifier = Modifier` as its first optional parameter and apply it to the root layout. Trigger when authoring or reviewing any `@Composable` function — including `private` / `internal` helpers extracted from a UI file.
---

## Rule

**Every `@Composable` function (public, internal, OR private) MUST declare a `modifier: Modifier = Modifier` parameter and pipe it into the outermost layout element.**

## Why

- Lets callers customise sizing, padding, click behavior, alignment, semantics, and test tags without forking the composable.
- Matches the official AndroidX Compose API guidelines so this codebase reads consistently with the rest of the ecosystem.
- Removes a footgun where you decompose a `*Ui.kt` into helpers and the helpers silently swallow caller intent.

## Position

`modifier` is the **first optional parameter**: it follows all required ones and precedes the rest of the defaults.

```kotlin
@Composable
internal fun PodcastRow(
  podcast: PodcastSearchResult,             // required
  onClick: () -> Unit,                       // required
  modifier: Modifier = Modifier,             // ← first default
  shape: Shape = MaterialTheme.shapes.medium,
)
```

## Apply to root, caller-first

Pipe `modifier` into the outermost layout. Chain local modifiers **onto** it (caller's modifications run first), don't `.then(modifier)` after locals.

```kotlin
@Composable
internal fun Section(
  text: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),       // caller first, ours after
    shape = MaterialTheme.shapes.medium,
  ) { Text(text) }
}
```

## Anti-patterns

- **No modifier parameter at all.** Most common offender — internal helpers like row composables, section headers, dialog bodies extracted from a screen UI.
  ```kotlin
  // ❌ DON'T
  @Composable
  private fun ExplicitRow(enabled: Boolean, onToggle: (Boolean) -> Unit) { ... }
  ```

- **Modifier not first among the defaults.** Pushes future API additions to insert other defaults before it and breaks call-site stability.
  ```kotlin
  // ❌ DON'T
  @Composable
  internal fun Row(
    text: String,
    shape: Shape = MaterialTheme.shapes.medium,
    modifier: Modifier = Modifier,
  )
  ```

- **Local-first then `.then(modifier)`.** Caller's intent is overridden by the composable's hardcoded modifiers.
  ```kotlin
  // ❌ DON'T
  Surface(modifier = Modifier.fillMaxWidth().then(modifier)) { ... }
  ```

- **Swallowed modifier.** Param accepted but never applied — usually leftover from a refactor.
  ```kotlin
  // ❌ DON'T
  @Composable
  internal fun Row(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = Modifier.fillMaxWidth()) { Text(text) }   // <-- ignored modifier
  }
  ```

## Not subject to this rule

- **`LazyListScope` / `LazyGridScope` / `BoxScope` extension functions** (e.g. `LazyListScope.episodesSection(...)`, `BoxScope.CenteredMessage(...)`) — these aren't standalone composables; they emit items into an existing scope and have no single root layout to attach a modifier to. If you need per-item modification, plumb it explicitly through the children.
- **Slot / content lambdas** typed `@Composable () -> Unit` — those are caller-supplied content, not functions you declare.
- **`@Preview` composables** — they don't take parameters from external callers, so a modifier param adds no value. (They should still be `private` or `internal`.)

## When to apply

- Authoring any new `@Composable` function — drop in `modifier: Modifier = Modifier` from the start.
- Reviewing a PR that introduces a Compose file or adds a new private helper composable inside one.
- **Extracting a private composable out of a screen UI file.** Tempting to skip the modifier param because "no one calls it except the file it came from" — don't. Tomorrow someone moves the file, or the screen needs to position the helper differently, or someone wants `Modifier.testTag(...)` on it. Future-you spends 30 seconds adding the parameter back; future-you will not remember why.
- Touching an existing helper that doesn't comply: opportunistically add `modifier: Modifier = Modifier` and apply it. Don't propagate the legacy shape when copy-pasting.

## Auditing the codebase

Find non-conforming composables:

```bash
# Composables with no parameter list mentioning Modifier
rg -P --multiline-dotall \
  '@Composable\s*\n(?:internal |private )?fun [A-Z]\w+\s*\([^)]*\)' \
  --type kt \
  -g '!*/build/*' \
  -g '!*Preview*' \
  | rg -v 'modifier:\s*Modifier'
```

The `add/composables/` and `builder/composables/` directories under `features/podcasts/ui/` carry the most violations as of the recent screen extractions — many helper rows, dialog bodies, and section composables were extracted without modifier params. Migrate as you touch them.
