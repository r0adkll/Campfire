// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

typealias CollectionId = String

data class Collection(
  val id: CollectionId,
  val name: String,
  val description: String?,
  val cover: String? = null,
  val coverFullPath: String? = null,
  val books: List<LibraryItem>,
  val updatedAt: Long,
  val createdAt: Long,
)
