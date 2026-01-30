package app.campfire.libraries.api.screen

import app.campfire.common.screens.BaseScreen
import app.campfire.core.filter.ContentFilter
import app.campfire.core.parcelize.Parcelize

@Parcelize
data class LibraryScreen(
  val filter: ContentFilter? = null,
) : BaseScreen(name = "Library")
