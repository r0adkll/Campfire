---
name: iconbutton-a11y
description: Enforce the Campfire IconButton accessibility convention — every IconButton (and its derivatives) must be wrapped with `IconButtonTooltip` and given a localized action label. Trigger when reviewing or authoring Compose UI that adds, copies, or moves an icon-only button.
---

## Rule

**Every `IconButton`, `FilledIconButton`, `OutlinedIconButton`, `FilledTonalIconButton`, `IconToggleButton`, `FilledIconToggleButton`, `FilledTonalIconToggleButton`, or `OutlinedIconToggleButton` MUST be wrapped with `IconButtonTooltip` from `app.campfire.common.compose.widgets`.**

The tooltip text MUST be a localized string from the module's `composeResources/values/*_strings.xml`, looked up via `stringResource(Res.string.<name>)`. The same string MUST also be passed to the inner `Icon`'s `contentDescription` so screen readers announce the action when the button is focused, in addition to revealing the tooltip on long-press / hover.

## When to apply

- Authoring any new icon-only button (Compose `material3.IconButton` or any derivative).
- Copy-pasting an existing icon-only button: re-check that it's wrapped — many older call sites in the codebase are NOT yet migrated; do not propagate the legacy pattern.
- Reviewing a PR that adds/moves an icon-only button in a `*Ui.kt`, top-app-bar, toolbar, bottom sheet, list-item action slot, or bottom bar.

## Component

```kotlin
// Located at:
// common/compose/src/commonMain/kotlin/app/campfire/common/compose/widgets/IconButtonTooltip.kt
package app.campfire.common.compose.widgets

@Composable
fun IconButtonTooltip(
  text: String,
  modifier: Modifier = Modifier,
  position: TooltipAnchorPosition = TooltipAnchorPosition.Above,
  state: TooltipState = rememberTooltipState(),
  content: @Composable () -> Unit,
)
```

Defaults are tuned for the common case (top app bar / toolbar action). Override `position` for buttons anchored below their tooltip (e.g. a bottom bar action) and `state` if you need to drive visibility programmatically.

## Canonical usage

```kotlin
import app.campfire.common.compose.widgets.IconButtonTooltip
import org.jetbrains.compose.resources.stringResource
import campfire.<module>.ui.generated.resources.Res
import campfire.<module>.ui.generated.resources.action_edit_playlist

IconButtonTooltip(
  text = stringResource(Res.string.action_edit_playlist),
) {
  IconButton(onClick = onEditClick) {
    Icon(
      Icons.Rounded.Edit,
      contentDescription = stringResource(Res.string.action_edit_playlist),
    )
  }
}
```

Toggle variant:

```kotlin
IconButtonTooltip(
  text = stringResource(Res.string.action_reorder_playlist),
) {
  IconToggleButton(
    checked = isReordering,
    onCheckedChange = onReorderChange,
  ) {
    Icon(
      CampfireIcons.Rounded.SwapCalls,
      contentDescription = stringResource(Res.string.action_reorder_playlist),
    )
  }
}
```

## Anti-patterns

- **Bare `IconButton` with no tooltip.** Fails Material 3 a11y guidance — long-press reveals nothing, screen readers may announce only "button".
  ```kotlin
  // ❌ DON'T
  IconButton(onClick = onClick) {
    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
  }
  ```

- **Hardcoded English label.** All tooltip text MUST go through a `stringResource(Res.string.*)` lookup; we have multiple locales planned.
  ```kotlin
  // ❌ DON'T
  IconButtonTooltip(text = "Edit playlist") { ... }
  ```

- **Inlining `TooltipBox` directly.** Use `IconButtonTooltip` so the plain-tooltip shape, padding, and anchor position stay consistent across the app.
  ```kotlin
  // ❌ DON'T
  TooltipBox(positionProvider = ..., tooltip = { PlainTooltip { Text("Edit") } }, ...) { ... }
  ```

- **Mismatched tooltip + contentDescription.** The two strings MUST be the same `stringResource` lookup. Drifting copy hurts a11y and looks sloppy.

## String resource conventions

- Place new tooltip labels in the **owning module's** `src/commonMain/composeResources/values/<module>_strings.xml`.
- Prefer `action_<verb>_<object>` or `cd_<noun>` naming, e.g. `action_edit_playlist`, `action_download_book`, `cd_clear_search`.
- Verbs in imperative mood: `Edit playlist`, `Delete bookmark`, `Toggle reorder`.

## Audit (as of 2026-05-11)

There are ~86 unmigrated `IconButton`/derivative call sites across 47 files. The wrapper is in place but call site migration is intentionally deferred. When you touch an unmigrated file for *any* reason, opportunistically migrate the IconButtons in it — do not propagate the legacy pattern when copy-pasting.

A representative reference implementation (already correctly tooltip-wrapped) lives at:
`features/playlists/ui/src/commonMain/kotlin/app/campfire/playlists/ui/detail/composables/PlaylistFloatingToolbar.kt`
(though that file inlines its own tooltip helper — migrate it to `IconButtonTooltip` when convenient).
