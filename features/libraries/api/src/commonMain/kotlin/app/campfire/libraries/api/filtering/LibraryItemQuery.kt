package app.campfire.libraries.api.filtering

import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.libraries.api.LibraryItemFilter

data class LibraryItemQuery(
  val filter: LibraryItemFilter? = null,
  val sortMode: SortMode = SortMode.Companion.Default,
  val sortDirection: SortDirection = SortDirection.Companion.Default,
)
