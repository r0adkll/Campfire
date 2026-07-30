// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.test.model

import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.Series

fun createNetworkSeries(
  id: String,
  name: String = "Network Series: $id",
  description: String? = null,
  addedAt: Long = 0L,
  updatedAt: Long = 0L,
  books: List<LibraryItemMinified.Book>? = null,
) = Series(
  id = id,
  name = name,
  description = description,
  addedAt = addedAt,
  updatedAt = updatedAt,
  books = books,
)
