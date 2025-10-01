package app.campfire.libraries.ui.list.sheets.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.campfire.core.logging.bark
import app.campfire.core.model.FilterData
import app.campfire.libraries.api.LibraryItemFilter
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.filter_category_author
import campfire.features.libraries.ui.generated.resources.filter_category_genre
import campfire.features.libraries.ui.generated.resources.filter_category_language
import campfire.features.libraries.ui.generated.resources.filter_category_missing
import campfire.features.libraries.ui.generated.resources.filter_category_narrator
import campfire.features.libraries.ui.generated.resources.filter_category_progress
import campfire.features.libraries.ui.generated.resources.filter_category_series
import campfire.features.libraries.ui.generated.resources.filter_category_tag
import campfire.features.libraries.ui.generated.resources.filter_category_tracks
import campfire.features.libraries.ui.generated.resources.filter_value_missing_asin
import campfire.features.libraries.ui.generated.resources.filter_value_missing_authors
import campfire.features.libraries.ui.generated.resources.filter_value_missing_description
import campfire.features.libraries.ui.generated.resources.filter_value_missing_genres
import campfire.features.libraries.ui.generated.resources.filter_value_missing_isbn
import campfire.features.libraries.ui.generated.resources.filter_value_missing_language
import campfire.features.libraries.ui.generated.resources.filter_value_missing_narrators
import campfire.features.libraries.ui.generated.resources.filter_value_missing_publishedYear
import campfire.features.libraries.ui.generated.resources.filter_value_missing_publisher
import campfire.features.libraries.ui.generated.resources.filter_value_missing_series
import campfire.features.libraries.ui.generated.resources.filter_value_missing_subtitle
import campfire.features.libraries.ui.generated.resources.filter_value_missing_tags
import campfire.features.libraries.ui.generated.resources.filter_value_progress_finished
import campfire.features.libraries.ui.generated.resources.filter_value_progress_in_progress
import campfire.features.libraries.ui.generated.resources.filter_value_progress_not_finished
import campfire.features.libraries.ui.generated.resources.filter_value_progress_not_started
import campfire.features.libraries.ui.generated.resources.filter_value_tracks_multi
import campfire.features.libraries.ui.generated.resources.filter_value_tracks_single
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
data class ItemFilterUiState(
  val selected: LibraryItemFilter?,
  val filters: ImmutableList<UiItemFilter<*>>,
  val eventSink: (ItemFilterUiEvent) -> Unit,
) : CircuitUiState

sealed interface UiItemFilter<Value : Any> {
  val name: StringResource
  val values: ImmutableList<Value>

  fun isValueSelectedFor(filter: LibraryItemFilter, value: Value): Boolean

  @Composable
  fun valueLabel(value: Value): String

  data class Genres(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_genre

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: String): Boolean {
      return when (filter) {
        is LibraryItemFilter.Genres -> {
          bark { "${filter.value} == $value" }
          value == filter.value
        }
        else -> false
      }
    }
  }

  data class Tags(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_tag

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: String): Boolean {
      return when (filter) {
        is LibraryItemFilter.Tags -> value == filter.value
        else -> false
      }
    }
  }

  data class Series(
    override val values: ImmutableList<FilterData.Entity>,
  ) : UiItemFilter<FilterData.Entity> {
    override val name = Res.string.filter_category_series

    @Composable
    override fun valueLabel(value: FilterData.Entity): String = value.name

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: FilterData.Entity): Boolean {
      return when (filter) {
        is LibraryItemFilter.Series -> value.id == filter.value
        else -> false
      }
    }
  }

  data class Authors(
    override val values: ImmutableList<FilterData.Entity>,
  ) : UiItemFilter<FilterData.Entity> {
    override val name = Res.string.filter_category_author

    @Composable
    override fun valueLabel(value: FilterData.Entity): String = value.name

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: FilterData.Entity): Boolean {
      return when (filter) {
        is LibraryItemFilter.Authors -> value.id == filter.value
        else -> false
      }
    }
  }

  data class Progress(
    override val values: ImmutableList<LibraryItemFilter.Progress.Type>,
  ) : UiItemFilter<LibraryItemFilter.Progress.Type> {
    override val name = Res.string.filter_category_progress

    @Composable
    override fun valueLabel(value: LibraryItemFilter.Progress.Type): String {
      return when (value) {
        LibraryItemFilter.Progress.Type.Finished -> stringResource(Res.string.filter_value_progress_finished)
        LibraryItemFilter.Progress.Type.NotStarted -> stringResource(Res.string.filter_value_progress_not_started)
        LibraryItemFilter.Progress.Type.NotFinished -> stringResource(Res.string.filter_value_progress_not_finished)
        LibraryItemFilter.Progress.Type.InProgress -> stringResource(Res.string.filter_value_progress_in_progress)
      }
    }

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: LibraryItemFilter.Progress.Type): Boolean {
      return when (filter) {
        is LibraryItemFilter.Progress -> value.value == filter.value
        else -> false
      }
    }
  }

  data class Narrators(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_narrator

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: String): Boolean {
      return when (filter) {
        is LibraryItemFilter.Narrators -> value == filter.value
        else -> false
      }
    }
  }

  data class Missing(
    override val values: ImmutableList<LibraryItemFilter.Missing.Type>,
  ) : UiItemFilter<LibraryItemFilter.Missing.Type> {
    override val name = Res.string.filter_category_missing

    @Composable
    override fun valueLabel(value: LibraryItemFilter.Missing.Type): String {
      return when (value) {
        LibraryItemFilter.Missing.Type.ASIN -> stringResource(Res.string.filter_value_missing_asin)
        LibraryItemFilter.Missing.Type.ISBN -> stringResource(Res.string.filter_value_missing_isbn)
        LibraryItemFilter.Missing.Type.SUBTITLE -> stringResource(Res.string.filter_value_missing_subtitle)
        LibraryItemFilter.Missing.Type.AUTHORS -> stringResource(Res.string.filter_value_missing_authors)
        LibraryItemFilter.Missing.Type.PUBLISHED_YEAR -> stringResource(Res.string.filter_value_missing_publishedYear)
        LibraryItemFilter.Missing.Type.SERIES -> stringResource(Res.string.filter_value_missing_series)
        LibraryItemFilter.Missing.Type.DESCRIPTION -> stringResource(Res.string.filter_value_missing_description)
        LibraryItemFilter.Missing.Type.GENRES -> stringResource(Res.string.filter_value_missing_genres)
        LibraryItemFilter.Missing.Type.TAGS -> stringResource(Res.string.filter_value_missing_tags)
        LibraryItemFilter.Missing.Type.NARRATORS -> stringResource(Res.string.filter_value_missing_narrators)
        LibraryItemFilter.Missing.Type.PUBLISHER -> stringResource(Res.string.filter_value_missing_publisher)
        LibraryItemFilter.Missing.Type.LANGUAGE -> stringResource(Res.string.filter_value_missing_language)
      }
    }

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: LibraryItemFilter.Missing.Type): Boolean {
      return when (filter) {
        is LibraryItemFilter.Missing -> value.value == filter.value
        else -> false
      }
    }
  }

  data class Languages(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_language

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: LibraryItemFilter, value: String): Boolean {
      return when (filter) {
        is LibraryItemFilter.Languages -> value == filter.value
        else -> false
      }
    }
  }

  data class Tracks(
    override val values: ImmutableList<LibraryItemFilter.Tracks.Type>,
  ) : UiItemFilter<LibraryItemFilter.Tracks.Type> {
    override val name = Res.string.filter_category_tracks

    @Composable
    override fun valueLabel(value: LibraryItemFilter.Tracks.Type): String {
      return when (value) {
        LibraryItemFilter.Tracks.Type.Single -> stringResource(Res.string.filter_value_tracks_single)
        LibraryItemFilter.Tracks.Type.Multi -> stringResource(Res.string.filter_value_tracks_multi)
      }
    }

    override fun isValueSelectedFor(
      filter: LibraryItemFilter,
      value: LibraryItemFilter.Tracks.Type,
    ): Boolean = when (filter) {
      is LibraryItemFilter.Tracks -> value.value == filter.value
      else -> false
    }
  }
}

sealed interface ItemFilterUiEvent : CircuitUiEvent {
  data object ClearFilter : ItemFilterUiEvent
  data class FilterSelected<Value : Any>(
    val filter: UiItemFilter<Value>,
    val value: Any,
  ) : ItemFilterUiEvent
}
