// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.data.LibraryItem
import app.campfire.data.Media

interface DatabaseAdapters {

  val libraryItemAdapter: LibraryItem.Adapter
  val mediaAdapter: Media.Adapter
}
