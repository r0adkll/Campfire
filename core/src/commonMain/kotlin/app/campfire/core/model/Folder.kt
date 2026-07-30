// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class Folder(
  val id: String,
  val fullPath: String,
  val libraryId: String,
  val addedAt: Long,
)
