package app.campfire.libraries.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.parcelize.Parcelize
import app.campfire.libraries.api.LibraryItemFilter

@Parcelize
data class LibraryScreen(
  val filter: LibraryItemFilter? = null,
) : BaseScreen(name = "Library")
