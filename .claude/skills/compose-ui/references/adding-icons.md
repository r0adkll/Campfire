# Adding an icon to CampfireIcons

Generates a Compose `ImageVector` from a Material Symbols Rounded SVG via the Valkyrie CLI.
For decorative theme icons (Icons8 Arcade), use the `add-theme-icons` skill instead — the
workflow below is for standard UI glyphs.

Prerequisite: Valkyrie CLI (`brew install ComposeGears/repo/valkyrie`, check with
`command -v valkyrie`). Generate with Valkyrie — hand-translating SVG paths is not acceptable.

## Step 1. Pick the pack and fill variant

- **`Rounded`, fill=0 (default)** — almost every glyph. This is the outline-style Material
  Symbols default and matches the app's established look (`Warning`, `Schedule`, `Delete`).
- **`Rounded`, fill=1 (solid)** — only for glyphs that must read as solid: player transport
  controls (`PlayArrow`, `Pause`, `Stop`, `SkipNext`, `FastForward`…) and the rating `Star`.
- **`Filled`, fill=1** — a nav item's selected state, always paired with an unselected fill=0
  icon of the same symbol in `Rounded` or `Outline` (precedent: `Filled.Settings` /
  `Rounded.Settings` in `NavigationPresenter`).
- Directional icons (arrows, lists, login/logout, trending, playlist, volume — anything that
  should flip in RTL) get `autoMirror = true` in Step 4.

## Step 2. Download the SVG

```bash
curl -s -o /tmp/campfire-icon-svg/<PascalCaseName>.svg \
  "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/<snake_name>/<default|fill1>/24px.svg"
```

- `<snake_name>` is the Material Symbols name (browse https://fonts.google.com/icons);
  `default` = fill 0, `fill1` = fill 1. Viewport is 960 — matches the rest of the pack.
- The SVG **filename** becomes the Kotlin property name (`CampfireIcons.Rounded.<PascalCaseName>`).
  Confirm the response contains `<svg` — a bad symbol name returns an HTML error page.

## Step 3. Run Valkyrie

```bash
valkyrie svgxml2imagevector \
  --input-path=/tmp/campfire-icon-svg \
  --output-path=/tmp/campfire-icon-out \
  --package-name=app.campfire.common.compose.icons \
  --iconpack-name=CampfireIcons \
  --nested-pack-name=Rounded \
  --output-format=lazy-property \
  --indent-size=2 \
  --trailing-comma=true
```

- `--nested-pack-name` selects the pack (`Rounded`/`Filled`); Valkyrie appends it lowercased
  to the package and writes into a matching subfolder, so pass the **root** package as shown.
- `--trailing-comma=true` keeps ktlint happy on multi-arg `path(...)` calls.
- `--output-format=lazy-property` emits the required `by lazy(LazyThreadSafetyMode.NONE)` shape.

## Step 4. Post-process the generated file

1. Prepend the license header:
   ```kotlin
   // Copyright 2026, Drew Heavner and the Campfire project contributors
   // SPDX-License-Identifier: GPL-3.0-only
   ```
2. Valkyrie names the builder `name = "Rounded.Foo"` — trim it to the bare icon name,
   `name = "Foo"`.
3. Directional icons: add `autoMirror = true,` to the `ImageVector.Builder(...)` arguments,
   after `viewportHeight`.

## Step 5. Move into the source tree

```
common/compose/src/commonMain/kotlin/app/campfire/common/compose/icons/<rounded|filled>/<Name>.kt
```

Use it as `CampfireIcons.<Pack>.<Name>` with imports for `CampfireIcons` and the extension
property. Icons for the ABS library-icon picker are additionally mapped in
`icons/LibraryIcon.kt`.

## Step 6. Verify

```bash
./scripts/ktlint --format
./gradlew :common:compose:compileKotlinJvm
```

Clean up `/tmp/campfire-icon-svg` and `/tmp/campfire-icon-out`.
