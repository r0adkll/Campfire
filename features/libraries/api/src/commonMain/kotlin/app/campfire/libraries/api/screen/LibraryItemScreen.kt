package app.campfire.libraries.api.screen

import app.campfire.common.screens.DetailScreen
import app.campfire.core.model.LibraryItemId
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class LibraryItemScreen(
  val libraryItemId: LibraryItemId,
) : DetailScreen(name = "LibraryItem()")
