---
name: compose-ui
description: Campfire Compose UI conventions. Trigger when authoring or reviewing Compose UI in this repo, and ALWAYS before adding, replacing, or importing an icon in UI code — every icon comes from the `CampfireIcons` pack, generated via the Valkyrie CLI (never from a material-icons library import, and never hand-written vector paths).
---

## When to use

- Building or editing a Compose screen, composable, or widget in Campfire.
- Any task that needs an icon: a new glyph, a replacement, or a reference the codebase doesn't have yet.

## Icons — the one rule

All icons are project-owned `ImageVector`s under
`common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/`, exposed as
extension properties on the `CampfireIcons` object. The deprecated
`material-icons-extended` library was removed in PR #1056 and must stay gone — the
dependency is not in the version catalog and no `androidx.compose.material.icons`
import may be reintroduced.

Packs:

| Pack | Use | Style |
|---|---|---|
| `CampfireIcons.Rounded` | Default for all UI glyphs | Material Symbols Rounded, fill=0 (fill=1 for solid-by-nature glyphs) |
| `CampfireIcons.Filled` | Nav selected states, paired with an unselected `Rounded`/`Outline` icon | fill=1 |
| `CampfireIcons.Outline` | Legacy unselected nav set | fill=0 |
| `CampfireIcons.Theme` | Decorative camping/nature icons for app themes | Icons8 Arcade — use the `add-theme-icons` skill instead |

Before adding, check the icon doesn't already exist:
`grep -r "val CampfireIcons" common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/`.
Many old material names were collapsed onto one asset (Clear→`Close`, WarningAmber→`Warning`,
ErrorOutline→`Error`, DeleteOutline→`Delete`) — search for the concept, not just the exact name.

To add a missing icon, follow [references/adding-icons.md](references/adding-icons.md) —
Valkyrie CLI generation from a Material Symbols SVG, with the project's post-processing rules
(license header, bare `name`, `autoMirror`, lazy pattern).

## Conventions

The Circuit screen/presenter/UI pattern, DI scopes, and module layout live in `CLAUDE.md` — follow them as written. On top of those:

- Every `ImageVector` is declared `by lazy(LazyThreadSafetyMode.NONE)` (Theme pack: `PUBLICATION`). The backing-property `private var _x` + `get()` pattern from older Valkyrie output is banned — convert it on sight.
- Every `@Composable` takes `modifier: Modifier = Modifier` as its first optional parameter (see the `compose-modifier` skill).
- Icon-only buttons are wrapped in `IconButtonTooltip` with a localized label (see the `iconbutton-a11y` skill).
- User-facing strings go through `composeResources` string resources (see the `i18n-compose` skill); no apostrophe escaping — write `doesn't`, never `doesn\'t`.

## Verify

- `./scripts/ktlint --format` (auto-fixes import ordering), then
- `./gradlew :common:compose:compileKotlinJvm` for icon-only changes, or the touched feature module's JVM compile task for UI changes. iOS link tasks can hang — compile tasks only.
