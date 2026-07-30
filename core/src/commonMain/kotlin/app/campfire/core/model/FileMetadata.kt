// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class FileMetadata(
  val filename: String,
  val ext: String,
  val path: String,
  val relPath: String,
  val size: Long,
  val mtimeMs: Long,
  val ctimeMs: Long,
  val birthtimeMs: Long,
)
