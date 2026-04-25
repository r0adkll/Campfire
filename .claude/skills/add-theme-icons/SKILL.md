---
name: add-theme-icons
description: Add new `AppTheme.Icon` entries by pulling Icons8 icons (Arcade style by default) and converting them to Compose ImageVectors via the Valkyrie CLI.
argument-hint: [concept terms or icon ids...]
---

## When to use

Invoke when the user wants to expand the set of icons available in `AppTheme.Icon` (used by OOTB `AppTheme.Fixed` themes and user-built `AppTheme.Fixed.Custom` themes). Icons should match the app's **camping / nature / travel** theme.

## Prerequisites

- **Valkyrie CLI** installed (`brew install ComposeGears/repo/valkyrie`, check with `command -v valkyrie`). Required — do not hand-translate SVG paths.
- **Icons8 MCP** tools available: `mcp__icons8mcp__search_icons`, `mcp__icons8mcp__get_icon_svg`.

## Conventions (project-specific — follow exactly)

| Concern | Value |
|---|---|
| Icon style (platform) | `arcade` (colored; unless user specifies otherwise) |
| Iconpack object | `CampfireIcons` (defined at `common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/CampfireIcons.kt`) |
| Nested pack | `Theme` |
| ImageVector package | `app.campfire.common.compose.icons.theme` |
| ImageVector files dir | `common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/theme/` |
| Lazy thread-safety | `LazyThreadSafetyMode.PUBLICATION` (Valkyrie emits `NONE` — **you must replace it**) |
| Indent | 2 spaces (ktlint) |
| Enum location | `ui/theming/api/src/commonMain/kotlin/app/campfire/ui/theming/api/AppTheme.kt` → `AppTheme.Icon` |

## Workflow

1. **Search candidates.** First read `AppTheme.kt` and list the existing `Icon` enum entries so you can exclude them. Then use `mcp__icons8mcp__search_icons` with `platform=arcade` and camping/nature/travel query terms (examples: campfire, compass, binoculars, lantern, map, axe, sleeping bag, evergreen, suitcase, log cabin, canoe, fishing, hammock, trail, camper, waterfall, flashlight). Note: some searches return zero (`kayak`, `hammock`, `thermos`) — try synonyms (`dinghy`, `picnic`). Fire multiple searches in parallel.

2. **Fetch SVGs in parallel** via `mcp__icons8mcp__get_icon_svg` for each chosen icon id.

3. **Stage SVGs.** Write each to `/tmp/campfire-icons-svg/<PascalCaseName>.svg`. The filename becomes the generated Kotlin property name (`CampfireIcons.Theme.<PascalCaseName>`) — avoid names that collide with existing Kotlin types (e.g. prefer `PaperMap` over `Map`).

4. **Run Valkyrie.** Output to a scratch dir first (Valkyrie creates a `theme/` subfolder):
   ```bash
   valkyrie svgxml2imagevector \
     --input-path=/tmp/campfire-icons-svg \
     --output-path=/tmp/campfire-icons-out \
     --package-name=app.campfire.common.compose.icons \
     --iconpack-name=CampfireIcons \
     --nested-pack-name=Theme \
     --output-format=lazy-property \
     --indent-size=2 \
     --trailing-comma=true
   ```
   Key flags: `--package-name` is the **root** package (Valkyrie appends `.theme` from `--nested-pack-name`); do **not** pass the full `...icons.theme` package or you'll get a doubled path. `--trailing-comma=true` is required — without it ktlint flags every multi-arg `path(...)` call.

5. **Post-process** — replace Valkyrie's default lazy mode to match project convention:
   ```bash
   bash -c 'for f in /tmp/campfire-icons-out/theme/*.kt; do sed -i "" "s/LazyThreadSafetyMode.NONE/LazyThreadSafetyMode.PUBLICATION/g" "$f"; done'
   ```

6. **Move into the source tree.**
   ```bash
   cp /tmp/campfire-icons-out/theme/*.kt \
      /Users/r0adkll/StudioProjects/Campfire/common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/theme/
   ```

7. **Wire into `AppTheme.Icon`.** In `ui/theming/api/src/commonMain/kotlin/app/campfire/ui/theming/api/AppTheme.kt`:
   - Add `import app.campfire.common.compose.icons.theme.<Name>` (keep alphabetical ordering).
   - Add `<Name>(icon = { CampfireIcons.Theme.<Name> }),` to the `Icon` enum.

8. **Verify.** Run `--format` first (it auto-fixes the alphabetical import reordering that adding new imports can break), then compile:
   ```bash
   ./scripts/ktlint --format
   ./gradlew :common:compose:compileKotlinIosSimulatorArm64 :ui:theming:api:compileKotlinIosSimulatorArm64 --console=plain
   ```
   Pre-existing warnings (`ExperimentalMaterial3Api` opt-in, `dayOfMonth` deprecation, `SynchronizedObject` under `compileCommonMainKotlinMetadata`) are unrelated — ignore.

9. **Clean up** `/tmp/campfire-icons-svg` and `/tmp/campfire-icons-out`.

## Notes

- Icons are rendered by `AppThemeImage` via `Image(...)` (not `Icon(... tint)`), so the original Arcade colors come through — do **not** convert to single-color vectors.
- Custom themes built by users pick from this same enum (see `AppTheme.Fixed.Custom.icon`), so every added entry is immediately user-selectable.
- If the user names a different Icons8 platform (e.g. `color`, `plumpy`), pass that as the `platform` filter in step 1 — the rest of the workflow is identical.
