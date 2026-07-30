// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class LibraryFile(
  val ino: String,
  val metadata: FileMetadata,
  val isSupplementary: Boolean? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val fileType: String,
)
