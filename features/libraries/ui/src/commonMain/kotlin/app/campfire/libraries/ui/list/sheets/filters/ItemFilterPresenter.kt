package app.campfire.libraries.ui.list.sheets.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.core.model.FilterData
import app.campfire.libraries.api.LibraryItemFilter
import app.campfire.libraries.api.filtering.FilteringRepository
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias ItemFilterPresenterFactory = (LibraryItemFilter?, (LibraryItemFilter?) -> Unit) -> ItemFilterPresenter

@Inject
class ItemFilterPresenter(
  @Assisted private val previousFilter: LibraryItemFilter?,
  @Assisted private val onFilterSelected: (LibraryItemFilter?) -> Unit,
  private val filteringRepository: FilteringRepository,
) : Presenter<ItemFilterUiState> {

  @Composable
  override fun present(): ItemFilterUiState {
    val filterData by remember {
      filteringRepository.observeFilterData()
    }.collectAsState(FilterData())

    return ItemFilterUiState(
      selected = previousFilter,
      filters = persistentListOf(
        UiItemFilter.Genres(filterData.genres.toPersistentList()),
        UiItemFilter.Tags(filterData.tags.toPersistentList()),
        UiItemFilter.Series(filterData.series.toPersistentList()),
        UiItemFilter.Authors(filterData.authors.toPersistentList()),
        UiItemFilter.Narrators(filterData.narrators.toPersistentList()),
        UiItemFilter.Languages(filterData.languages.toPersistentList()),

        UiItemFilter.Progress(
          values = LibraryItemFilter.Progress.Type.entries.toPersistentList(),
        ),

        UiItemFilter.Missing(
          values = LibraryItemFilter.Missing.Type.entries.toPersistentList(),
        ),

        UiItemFilter.Tracks(
          values = LibraryItemFilter.Tracks.Type.entries.toPersistentList(),
        ),
      ),
    ) { event ->
      when (event) {
        is ItemFilterUiEvent.FilterSelected<*> -> {
          val newFilter = when (event.filter) {
            is UiItemFilter.Authors -> {
              val entity = event.value as FilterData.Entity
              LibraryItemFilter.Authors(entity.id, entity.name)
            }
            is UiItemFilter.Genres -> LibraryItemFilter.Genres(event.value as String)
            is UiItemFilter.Languages -> LibraryItemFilter.Languages(event.value as String)
            is UiItemFilter.Missing -> LibraryItemFilter.Missing(event.value as LibraryItemFilter.Missing.Type)
            is UiItemFilter.Narrators -> LibraryItemFilter.Narrators(event.value as String)
            is UiItemFilter.Progress -> LibraryItemFilter.Progress(event.value as LibraryItemFilter.Progress.Type)
            is UiItemFilter.Series -> {
              val entity = event.value as FilterData.Entity
              LibraryItemFilter.Series(entity.id, entity.name)
            }
            is UiItemFilter.Tags -> LibraryItemFilter.Tags(event.value as String)
            is UiItemFilter.Tracks -> LibraryItemFilter.Tracks(event.value as LibraryItemFilter.Tracks.Type)
          }
          onFilterSelected(newFilter)
        }

        ItemFilterUiEvent.ClearFilter -> onFilterSelected(null)
      }
    }
  }
}
