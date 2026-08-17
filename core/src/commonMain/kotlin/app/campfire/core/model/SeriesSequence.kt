// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class SeriesSequence(
  val id: String,
  val name: String,
  val sequence: Int,
)

/**
 * Canonical ordering for the series a book belongs to. The Audiobookshelf server builds
 * a book's series array from an unordered join, so its order can differ between requests;
 * sort by name (then id) so the list is deterministic everywhere it's shown or stored.
 */
fun List<SeriesSequence>.sortedByName(): List<SeriesSequence> {
  return sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
}
