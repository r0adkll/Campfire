package app.campfire.series.paging

import app.campfire.core.filter.ContentFilter
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.SortDirection

/**
 * This keys any paging list of library items by its root request, in that
 * cached pages/lists of library items are unique to these input parameters.
 *
 * If the user changes the filter or sorting configuration then we need to start loading
 * a unique list of paged content.
 */
data class SeriesPagingInput(
  val filter: ContentFilter?,
  val sortMode: ContentSortMode,
  val sortDirection: SortDirection,
) {
  // Generate the key used to serialize pages / requests in the database
  val databaseKey = buildString {
    filter?.let {
      append(filter.group)
      append(filter.value)
    }
    append(sortMode.storageKey)
    append(sortDirection.storageKey)
  }
}
