// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder

/** The tools the builder's toolbar switches between on a phone, each with a tray of its own. */
internal enum class BuilderTool {
  /** The seed color: curated swatches, the user's books, or a color of their own. */
  Color,

  /** The style cards, and contrast. */
  Style,

  /** The five palette colors. */
  Palette,

  /** Setting exact colors for a part of the app. */
  FineTune,
}
