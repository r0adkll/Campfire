// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.layout.isHeightCompact
import app.campfire.common.compose.layout.isWidthAtLeastMedium

/** How an adaptive sheet lays itself out over the region that hosts it. */
enum class SheetPresentation {
  /** Across the bottom edge, the full width of the region. The default everywhere tall enough. */
  Bottom,

  /** Down the trailing edge, the full height of the region. For wide, short regions. */
  Side,

  /** A card centred in the region. For regions too short for a sheet and too narrow for a panel. */
  Dialog,
}

/**
 * Which presentation a region of this size gets.
 *
 * Height decides first: anything with the room for it keeps the bottom sheet, which is what the
 * content was written for. A short region cannot — half of it is roughly two list rows — so the
 * sheet turns on its side where there is the width for a panel, and becomes a card where there is
 * not.
 */
fun WindowSizeClass.sheetPresentation(): SheetPresentation = when {
  !isHeightCompact -> SheetPresentation.Bottom
  isWidthAtLeastMedium -> SheetPresentation.Side
  else -> SheetPresentation.Dialog
}

/**
 * The presentation the surrounding sheet chose, for content that has to lay itself out differently
 * inside one — a drag handle that turns vertical, a title that stops being centred.
 *
 * Content rendered outside a sheet reads [SheetPresentation.Bottom], which is the arrangement the
 * sheet bodies were written against.
 */
val LocalSheetPresentation = compositionLocalOf { SheetPresentation.Bottom }
