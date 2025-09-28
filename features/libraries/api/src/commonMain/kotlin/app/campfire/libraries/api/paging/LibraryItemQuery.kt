package app.campfire.libraries.api.paging

import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortMode
import app.campfire.libraries.api.LibraryItemFilter

data class LibraryItemQuery(
  val filter: LibraryItemFilter? = null,
  val sortMode: SortMode = SortMode.Default,
  val sortDirection: SortDirection = SortDirection.Default,
)
