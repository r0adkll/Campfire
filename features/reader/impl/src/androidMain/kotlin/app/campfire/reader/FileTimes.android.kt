// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import java.io.File
import kotlinx.io.files.Path

internal actual fun Path.modifiedAtMs(): Long = File(toString()).lastModified().takeIf { it > 0L } ?: -1L

internal actual fun Path.setModifiedAtMs(epochMs: Long) {
  File(toString()).setLastModified(epochMs)
}
