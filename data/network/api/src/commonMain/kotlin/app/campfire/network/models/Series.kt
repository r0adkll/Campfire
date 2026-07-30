// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A series object which includes the name and description of the series.
 *
 * @param id The ID of the series.
 * @param name The name of the series.
 * @param libraryId The ID of the library that the series belongs to.
 * @param description A description for the series. Will be null if there is none.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 */
@Serializable
data class Series(
  val id: String,
  val name: String,
  val libraryId: String? = null,
  val description: String? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val books: List<LibraryItemMinified.Book>? = null,
)
