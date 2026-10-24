// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.common.screens.WorkspaceScreen
import app.campfire.core.model.LibraryItemId
import app.campfire.core.parcelize.Parcelize
import app.campfire.libraries.api.screen.LibraryItemScreen

/** Where an account allowed to change items edits a book's details on the server. */
@Parcelize
data class ItemEditorScreen(
  val libraryItemId: LibraryItemId,
) : WorkspaceScreen(name = "ItemEditor") {

  override val preview: DetailScreen
    get() = LibraryItemScreen(libraryItemId)
}
