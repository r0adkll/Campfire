// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.api.model

/**
 * A shelf's saved place in a user's customized home layout.
 *
 * @param label the last label seen for the shelf, so the editor can still name a server shelf
 *  that isn't in the current response (e.g. because it's empty right now)
 */
data class HomeLayoutEntry(
  val shelfId: ShelfId,
  val visible: Boolean,
  val label: String,
)

/**
 * A shelf that Home can show right now, listed in its default order.
 */
data class AvailableShelf(
  val id: ShelfId,
  val label: String,
  val defaultVisible: Boolean = true,
)

/**
 * A shelf in a resolved [HomeLayout].
 *
 * @param isAvailable false for a saved shelf that Home can't show right now; it keeps its slot
 */
data class HomeLayoutShelf(
  val id: ShelfId,
  val label: String,
  val visible: Boolean,
  val isAvailable: Boolean,
)

/**
 * Every shelf Home knows about for a library, in display order.
 *
 * @param isCustomized whether the user has saved a layout for this library
 */
data class HomeLayout(
  val shelves: List<HomeLayoutShelf>,
  val isCustomized: Boolean,
) {

  /** The shelves Home renders, in order. */
  val visibleShelves: List<HomeLayoutShelf>
    get() = shelves.filter { it.visible && it.isAvailable }

  /** The entries to persist for this layout. */
  fun toEntries(): List<HomeLayoutEntry> = shelves.map { HomeLayoutEntry(it.id, it.visible, it.label) }
}

/**
 * The shelves Home can show in their default order: the server's feed, with the client-side
 * upcoming releases shelf placed after Discover (or at the end without one).
 */
fun defaultAvailableShelves(serverShelves: List<Shelf>): List<AvailableShelf> {
  val shelves = serverShelves.map { AvailableShelf(it.id, it.label) }
  val upcoming = AvailableShelf(ShelfIds.UpcomingReleases, label = "")
  val anchor = shelves.indexOfFirst { it.id == ShelfIds.Discover }
  return if (anchor >= 0) {
    shelves.toMutableList().apply { add(anchor + 1, upcoming) }
  } else {
    shelves + upcoming
  }
}

/**
 * Merge a user's [saved] layout with the shelves that are [available] now.
 *
 * - Without a saved layout, Home follows [available] exactly.
 * - A saved shelf keeps its slot and visibility; if it isn't available it stays in the layout
 *   (so the editor can still show and move it) but Home doesn't render it.
 * - An available shelf the saved layout doesn't know about lands right after the nearest shelf
 *   before it in [available] that the layout does know, or at the top when there is none, with
 *   its [AvailableShelf.defaultVisible]. Nothing is written back; the placement is re-derived on
 *   every call until the user saves a layout that includes it.
 */
fun resolveHomeLayout(
  saved: List<HomeLayoutEntry>?,
  available: List<AvailableShelf>,
): HomeLayout {
  val availableById = available.associateBy { it.id }
  if (saved == null) {
    return HomeLayout(
      shelves = available.map { it.toLayoutShelf(it.defaultVisible) },
      isCustomized = false,
    )
  }

  val shelves = saved
    .distinctBy { it.shelfId }
    .map { entry ->
      val current = availableById[entry.shelfId]
      HomeLayoutShelf(
        id = entry.shelfId,
        label = current?.label ?: entry.label,
        visible = entry.visible,
        isAvailable = current != null,
      )
    }
    .toMutableList()

  var previousKnownId: ShelfId? = null
  available.forEach { shelf ->
    if (shelves.none { it.id == shelf.id }) {
      val insertAt = previousKnownId
        ?.let { id -> shelves.indexOfFirst { it.id == id } + 1 }
        ?: 0
      shelves.add(insertAt, shelf.toLayoutShelf(shelf.defaultVisible))
    }
    previousKnownId = shelf.id
  }

  return HomeLayout(shelves = shelves, isCustomized = true)
}

private fun AvailableShelf.toLayoutShelf(visible: Boolean) = HomeLayoutShelf(
  id = id,
  label = label,
  visible = visible,
  isAvailable = true,
)
