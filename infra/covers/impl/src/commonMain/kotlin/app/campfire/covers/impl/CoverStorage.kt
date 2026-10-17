// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

/** Where kept covers live: beside the downloads, out of the cache the OS may clear. */
fun interface CoverStorage {
  fun directory(): String
}
