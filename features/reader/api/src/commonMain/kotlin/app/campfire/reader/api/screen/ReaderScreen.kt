// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.api.screen

import app.campfire.common.screens.WindowScreen
import app.campfire.core.model.LibraryItemId
import app.campfire.core.parcelize.Parcelize

/** Reads the primary ebook of [libraryItemId], in a window of its own above the app. */
@Parcelize
data class ReaderScreen(
  val libraryItemId: LibraryItemId,
) : WindowScreen(name = "Reader")
