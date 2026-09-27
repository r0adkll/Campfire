// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.home

import app.campfire.home.api.model.HomeLayoutShelf
import app.campfire.home.api.model.ShelfId

/*
 * Edits the home layout editor makes. The editor keeps the layout as its shown shelves, in order,
 * followed by its hidden ones, so each edit returns the whole list in that shape, ready to save.
 */

internal val List<HomeLayoutShelf>.shownShelves: List<HomeLayoutShelf>
  get() = filter { it.visible }

internal val List<HomeLayoutShelf>.hiddenShelves: List<HomeLayoutShelf>
  get() = filterNot { it.visible }

/** Move the shown shelf at [from] to [to], both indices into [shownShelves]. */
internal fun List<HomeLayoutShelf>.moveShown(from: Int, to: Int): List<HomeLayoutShelf> {
  val shown = shownShelves.toMutableList()
  if (from !in shown.indices || to !in shown.indices) return shownShelves + hiddenShelves
  shown.add(to, shown.removeAt(from))
  return shown + hiddenShelves
}

/** Move the shown shelf [id] by [offset] places, clamped to the shown section. */
internal fun List<HomeLayoutShelf>.moveShownBy(id: ShelfId, offset: Int): List<HomeLayoutShelf> {
  val from = shownShelves.indexOfFirst { it.id == id }
  if (from < 0) return shownShelves + hiddenShelves
  return moveShown(from, (from + offset).coerceIn(0, shownShelves.lastIndex))
}

/**
 * Show or hide shelf [id]. A shelf that's shown joins the end of the shown section; a hidden one
 * goes to the top of the hidden section, so either way it lands next to where the user tapped.
 */
internal fun List<HomeLayoutShelf>.setVisible(id: ShelfId, visible: Boolean): List<HomeLayoutShelf> {
  val shelf = firstOrNull { it.id == id } ?: return shownShelves + hiddenShelves
  val others = filterNot { it.id == id }
  return others.shownShelves + shelf.copy(visible = visible) + others.hiddenShelves
}
