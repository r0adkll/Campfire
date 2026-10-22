// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import kotlinx.io.files.Path

// kotlinx-io's file metadata has no times

/** When the file was last modified, in epoch milliseconds, or -1 if that can't be read. */
internal expect fun Path.modifiedAtMs(): Long

/** Mark the file as last modified at [epochMs]. */
internal expect fun Path.setModifiedAtMs(epochMs: Long)
