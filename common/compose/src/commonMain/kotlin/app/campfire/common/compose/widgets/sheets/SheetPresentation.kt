// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.sheets

import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.layout.isHeightCompact

/** How an adaptive sheet lays itself out over the region that hosts it. */
enum class SheetPresentation {
  /** Across the bottom edge, the full width of the region. The default everywhere tall enough. */
  Bottom,

  /** Down the trailing edge, the full height of the region. For regions too short for a sheet. */
  Side,
}

/**
 * Which presentation a region of this size gets.
 *
 * Only height decides. A region with the room for a bottom sheet keeps one, which is what the
 * content was written for; a short region cannot, since half of it comes to roughly two list rows,
 * so the sheet turns on its side and takes the height instead. The side sheet sizes itself to its
 * content, so there is no width below which it stops working — a narrow region simply gets a sheet
 * that covers more of it, the same way a bottom sheet on a small phone fills the width.
 */
fun WindowSizeClass.sheetPresentation(): SheetPresentation =
  if (isHeightCompact) SheetPresentation.Side else SheetPresentation.Bottom

/**
 * The presentation the surrounding sheet chose, for content that has to lay itself out differently
 * inside one — a drag handle that turns vertical, a title that stops being centred.
 *
 * Content rendered outside a sheet reads [SheetPresentation.Bottom], which is the arrangement the
 * sheet bodies were written against.
 */
val LocalSheetPresentation = compositionLocalOf { SheetPresentation.Bottom }
