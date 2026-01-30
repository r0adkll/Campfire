package app.campfire.filters.ui.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.FilterData
import app.campfire.filters.AllowedFilterCategories
import app.campfire.filters.FilteringRepository
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ContentFilterPresenterFactory = (
  filter: ContentFilter?,
  allowedCategories: AllowedFilterCategories?,
  onFilterSelected: (ContentFilter?) -> Unit,
) -> ContentFilterPresenter

@Inject
class ContentFilterPresenter(
  @Assisted private val previousFilter: ContentFilter?,
  @Assisted private val allowedCategories: AllowedFilterCategories?,
  @Assisted private val onFilterSelected: (ContentFilter?) -> Unit,
  private val filteringRepository: FilteringRepository,
) : Presenter<ContentFilterUiState> {

  @Composable
  override fun present(): ContentFilterUiState {
    val filterData by remember {
      filteringRepository.observeFilterData()
    }.collectAsState(FilterData())

    return ContentFilterUiState(
      selected = previousFilter,
      filters = listOf(
        UiItemFilter.Genres(filterData.genres.toPersistentList()),
        UiItemFilter.Tags(filterData.tags.toPersistentList()),
        UiItemFilter.Series(filterData.series.toPersistentList()),
        UiItemFilter.Authors(filterData.authors.toPersistentList()),
        UiItemFilter.Narrators(filterData.narrators.toPersistentList()),
        UiItemFilter.Publishers(filterData.publishers.toPersistentList()),
        UiItemFilter.Languages(filterData.languages.toPersistentList()),

        UiItemFilter.Progress(
          values = ContentFilter.Progress.Type.entries.toPersistentList(),
        ),

        UiItemFilter.Missing(
          values = ContentFilter.Missing.Type.entries.toPersistentList(),
        ),

        UiItemFilter.Tracks(
          values = ContentFilter.Tracks.Type.entries.toPersistentList(),
        ),
      ).filter { itemFilter ->
        allowedCategories?.isAllowed(itemFilter::class) ?: true
      }.toPersistentList(),
    ) { event ->
      when (event) {
        is ItemFilterUiEvent.FilterSelected<*> -> {
          val newFilter = when (event.filter) {
            is UiItemFilter.Authors -> {
              val entity = event.value as FilterData.Entity
              ContentFilter.Authors(entity.id, entity.name)
            }
            is UiItemFilter.Genres -> ContentFilter.Genres(event.value as String)
            is UiItemFilter.Languages -> ContentFilter.Languages(event.value as String)
            is UiItemFilter.Missing -> ContentFilter.Missing(event.value as ContentFilter.Missing.Type)
            is UiItemFilter.Narrators -> ContentFilter.Narrators(event.value as String)
            is UiItemFilter.Publishers -> ContentFilter.Publishers(event.value as String)
            is UiItemFilter.Progress -> ContentFilter.Progress(event.value as ContentFilter.Progress.Type)
            is UiItemFilter.Series -> {
              val entity = event.value as FilterData.Entity
              ContentFilter.Series(entity.id, entity.name)
            }
            is UiItemFilter.Tags -> ContentFilter.Tags(event.value as String)
            is UiItemFilter.Tracks -> ContentFilter.Tracks(event.value as ContentFilter.Tracks.Type)
          }
          onFilterSelected(newFilter)
        }

        ItemFilterUiEvent.ClearFilter -> onFilterSelected(null)
      }
    }
  }
}
