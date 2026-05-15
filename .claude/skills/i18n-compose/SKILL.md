---
name: i18n-compose
description: Extract hardcoded user-facing string literals from a Jetpack Compose / Compose Multiplatform file into the owning module's `composeResources/values/<module>_strings.xml`, then replace each call site with `stringResource(Res.string.<key>, ...)` and add the matching `Res` / key imports. Trigger when the user asks to "localize", "i18n", "extract strings", or "move strings to resources" for a given Compose file.
argument-hint: [file] [--apply|--plan]
---

## What this skill does

Given a Compose Kotlin file (typically `*Ui.kt` or a composable in `features/**/ui/...` or `ui/**/`), this skill:

1. **Locates the owning module** by walking up to the nearest `src/commonMain/composeResources/values/*_strings.xml` (or `src/commonMain/composeResources/values/` if the strings file does not yet exist — derive the filename from the module path).
2. **Finds user-facing string literals** in `@Composable` code: `Text("...")`, `contentDescription = "..."`, `placeholder = { Text("...") }`, tooltip `text = "..."`, error messages, dialog titles, etc. Includes string-template literals (`"$count items"`) — convert templates to format args.
3. **Proposes a key for each literal**, following the existing naming conventions in the module's `_strings.xml`. Group by feature/screen with a stable prefix (e.g. `action_*`, `cd_*`, `<screen>_<purpose>`).
4. **Presents a plan** as a table: literal → proposed key → XML value (with placeholders) → call-site replacement. Wait for user approval unless invoked with `--apply`.
5. **Applies the changes** by editing the strings XML, the source file (replacing each literal), and adding/sorting the imports.
6. **Runs ktlint** on the touched files and reports any unresolved formatting issues.

## When to invoke

- User says: "localize this file", "i18n this", "extract strings from `<file>`", "move these strings to resources", "/i18n-compose <path>".
- Reviewing a new `*Ui.kt` PR that contains hardcoded English literals.
- Migrating an older legacy screen to the resource pattern.

Do NOT invoke for:
- Non-UI Kotlin code (presenters, repositories, network code). Strings used outside `@Composable` need a different lookup (`getString(Res.string.x)` is `suspend`); flag those for the user instead of silently converting.
- Pure test files, debug-only screens behind a debug flag, or strings the user has explicitly tagged as developer-only / non-translatable.

## Locate the strings.xml

For a file at `features/podcasts/ui/src/commonMain/kotlin/app/campfire/podcasts/ui/find/FindEpisodesUi.kt`:

1. Walk up to the module root (`features/podcasts/ui/`).
2. Strings file lives at `features/podcasts/ui/src/commonMain/composeResources/values/<module>_strings.xml`.
3. Filename convention is `<dot-separated module path>_strings.xml` with `:` and `-` swapped for `_` (e.g. `:features:podcasts:ui` → `podcasts_ui_strings.xml`, `:ui:appbar` → `appbar_strings.xml`). When in doubt, peek at sibling modules — the filename pattern is consistent within `features/`, `ui/`, and `infra/`.
4. The generated import package is `campfire.<dot-path>.generated.resources`. Examples:
   - `:features:podcasts:ui` → `campfire.features.podcasts.ui.generated.resources`
   - `:ui:appbar` → `campfire.ui.appbar.generated.resources`
   - `:app:common` → `campfire.app.common.generated.resources`
5. If the strings file does not yet exist, create the directory `src/commonMain/composeResources/values/` and seed the file with `<resources>\n</resources>`.

## Identify literals

Scan the file's AST visually (or via grep + careful read) for:

- `Text("literal")` and `Text(text = "literal", ...)`
- `contentDescription = "literal"` and `contentDescription = "literal $variable"`
- Tooltip/dialog text: `IconButtonTooltip(text = "...")`, `AlertDialog(title = { Text("...") }, text = { Text("...") })`
- Placeholder/label slots in TextField, SearchBar: `placeholder = { Text("...") }`, `label = { Text("...") }`
- Snackbar messages, error strings used directly in UI
- `Modifier.semantics { contentDescription = "..." }`

**Skip:**
- Strings already wrapped in `stringResource(...)` or `pluralStringResource(...)`.
- Test tags (`Modifier.testTag("...")`).
- Log tags, route IDs, JSON keys, analytics event names, navigation deep-link paths.
- Format-string placeholders that are themselves Compose resources.
- Empty strings (`""`) and single-character separators (`" "`, `"·"`, `"•"`).
- Strings already obviously developer-only (TODO markers, debug surfaces).

If unsure, flag the literal in the plan with `???` and ask the user.

### String templates → format args

Convert `"$count items"` and `"${user.name}'s library"` to positional placeholders:

- `"$count items"` → XML: `"%1$d items"` (assume `Int` for `count*`/`*count`/`size`/`*Size`) or `"%1$s items"` if the type is ambiguous. Call site: `stringResource(Res.string.<key>, count)`.
- `"${user.name}'s library"` → XML: `"%1$s's library"`. Call site: `stringResource(Res.string.<key>, user.name)`.
- Multi-arg: `"$first of $total"` → `"%1$d of %2$d"`, call site `stringResource(Res.string.<key>, first, total)`.

XML must escape `%` as `%%` and escape `"` and `\\` and `'` per Android string rules. Use `\"` for inner quotes and wrap values containing leading/trailing whitespace in `"..."`.

## Naming conventions

Follow the existing keys in the target `_strings.xml`. Common prefixes:

- `action_<verb>_<object>` — button/tooltip labels: `action_mark_episode_finished`
- `cd_<noun>` — content descriptions: `cd_clear_search`, `cd_book_cover`
- `<screen>_<purpose>` — screen-scoped copy: `find_episodes_empty`, `latest_episodes_title`
- `error_<what>` — error states: `error_load_failed`
- `dialog_<name>_<part>` — dialog title/body/confirm/dismiss
- Plurals: prefer ICU-style placeholders for now (Compose Resources `pluralStringResource` is supported — propose `plural_<noun>` only if the file already uses one).

Verbs are imperative ("Edit playlist", not "Editing playlist"). Sentence case for body copy, Title Case for short titles/buttons — match what's already in the file.

If the proposed key collides with an existing key in the XML:
- If the existing value is identical → reuse the key.
- If the existing value differs → append a disambiguating suffix (`_short`, `_screen`, `<feature>`) and call it out in the plan.

## Plan output

Present the plan as a single markdown table before applying anything:

```
| Line | Literal                              | Proposed key                  | XML value              | Notes |
|------|--------------------------------------|-------------------------------|------------------------|-------|
| 42   | "Latest"                             | latest_episodes_title         | Latest                 |       |
| 87   | "No episodes match \"$query\""       | find_episodes_empty           | No episodes match \"%1$s\" | template → arg |
| 91   | contentDescription = "${book.title}" | cd_book_cover                 | %1$s cover             |       |
```

Below the table, list:
- New imports to add (sorted, deduped) and any to remove.
- Whether a new `_strings.xml` will be created.
- Any literals you skipped and why (e.g. test tag, log tag).
- Any literals flagged `???` that need user input.

Pause for user approval unless `--apply` was passed. If the user approves, apply all edits in one pass.

## Apply step

1. **XML edits**: append new `<string name="...">value</string>` entries. Keep entries grouped by feature/screen (look at the existing blank-line groupings and follow them). Do not re-sort existing entries.
2. **Source edits**: replace each literal with `stringResource(Res.string.<key>)` or `stringResource(Res.string.<key>, arg1, arg2)`. For call sites where the result needs to be captured (e.g. passed to a non-composable lambda), hoist to a `val foo = stringResource(...)` near the top of the surrounding `@Composable`.
3. **Imports**: add `import org.jetbrains.compose.resources.stringResource` if missing, `import <pkg>.generated.resources.Res`, and one `import <pkg>.generated.resources.<key>` per new key. Keep imports sorted; ktlint will fix order on the final pass if needed.
4. **Format**: run `./scripts/ktlint --format <touched files>` and report the result. If ktlint complains beyond import sorting, surface the message — don't silently mass-edit.

## Non-composable call sites

If a literal sits outside a `@Composable` function (e.g. a `presenter`-side error message that flows into UI state):

- DO extract the key into the strings XML.
- Do NOT swap the call site to `stringResource(...)` — that's a `@Composable` function.
- Instead, surface the literal at the UI layer: either (a) move the lookup to the UI by passing a sealed `ErrorReason` through state and resolving at the `@Composable` boundary, or (b) use the suspending `getString(Res.string.<key>)` from `org.jetbrains.compose.resources` if the call site is already suspend.
- Flag this in the plan with `Notes: non-composable — move resolution to UI` and let the user decide.

## Examples

### Before

```kotlin
// features/podcasts/ui/src/commonMain/kotlin/.../FindEpisodesUi.kt
@Composable
fun FindEpisodesUi(state: FindEpisodesUiState) {
  Scaffold(
    topBar = {
      TopAppBar(title = { Text("Find episodes") })
    },
  ) {
    if (state.results.isEmpty()) {
      Text("No episodes match \"${state.query}\"")
    }
    IconButton(onClick = { /*...*/ }) {
      Icon(Icons.Rounded.Close, contentDescription = "Clear search")
    }
  }
}
```

### Plan

```
| Line | Literal                                       | Proposed key                  | XML value                       | Notes |
|------|-----------------------------------------------|-------------------------------|---------------------------------|-------|
| 6    | "Find episodes"                               | find_episodes_title           | Find episodes                   |       |
| 10   | "No episodes match \"${state.query}\""        | find_episodes_empty           | No episodes match \"%1$s\"      | template |
| 13   | "Clear search"                                | cd_clear_search               | Clear search                    |       |
```

New imports:
```
campfire.features.podcasts.ui.generated.resources.Res
campfire.features.podcasts.ui.generated.resources.cd_clear_search
campfire.features.podcasts.ui.generated.resources.find_episodes_empty
campfire.features.podcasts.ui.generated.resources.find_episodes_title
org.jetbrains.compose.resources.stringResource
```

### After

```kotlin
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.cd_clear_search
import campfire.features.podcasts.ui.generated.resources.find_episodes_empty
import campfire.features.podcasts.ui.generated.resources.find_episodes_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun FindEpisodesUi(state: FindEpisodesUiState) {
  Scaffold(
    topBar = {
      TopAppBar(title = { Text(stringResource(Res.string.find_episodes_title)) })
    },
  ) {
    if (state.results.isEmpty()) {
      Text(stringResource(Res.string.find_episodes_empty, state.query))
    }
    IconButtonTooltip(text = stringResource(Res.string.cd_clear_search)) {
      IconButton(onClick = { /*...*/ }) {
        Icon(
          Icons.Rounded.Close,
          contentDescription = stringResource(Res.string.cd_clear_search),
        )
      }
    }
  }
}
```

> Note: this example also wraps the `IconButton` in `IconButtonTooltip` per the [iconbutton-a11y](../iconbutton-a11y/SKILL.md) convention. When extracting strings, opportunistically migrate icon-only buttons in the same pass.

## Anti-patterns

- **Inventing keys without checking the file**. Always read the target `_strings.xml` first and reuse prefixes already present.
- **Extracting log tags / test tags / route IDs.** Those are developer-only strings, never translated.
- **Silent template flattening.** Always convert `"$x"` to `%1$s`/`%1$d` (etc.) and pass the value through `stringResource(...)` args — never `stringResource(Res.string.key) + " " + x`.
- **Mixing positional and non-positional placeholders.** Always use positional (`%1$s`, `%2$d`) — required for translators reordering args.
- **Mass-applying without showing a plan first.** Unless `--apply` was explicitly passed, always pause for user approval after the plan table.
- **Forgetting to add the `Res` import.** Each new key needs its own import line; the bare `Res` import alone won't compile.

## Verification

After applying:
1. The strings XML parses (run `xmllint --noout <file>` if available, else eyeball balanced tags).
2. `./scripts/ktlint --check <touched-files>` is clean.
3. Optional: `./gradlew :<module-path>:compileKotlinMetadata` (or the appropriate compile task) to confirm the generated resources picked up the new keys.
