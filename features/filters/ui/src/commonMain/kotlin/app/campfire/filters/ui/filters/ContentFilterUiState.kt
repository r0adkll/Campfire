package app.campfire.filters.ui.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.FilterData
import campfire.features.filters.ui.generated.resources.Res
import campfire.features.filters.ui.generated.resources.filter_category_author
import campfire.features.filters.ui.generated.resources.filter_category_genre
import campfire.features.filters.ui.generated.resources.filter_category_language
import campfire.features.filters.ui.generated.resources.filter_category_missing
import campfire.features.filters.ui.generated.resources.filter_category_narrator
import campfire.features.filters.ui.generated.resources.filter_category_progress
import campfire.features.filters.ui.generated.resources.filter_category_publisher
import campfire.features.filters.ui.generated.resources.filter_category_series
import campfire.features.filters.ui.generated.resources.filter_category_tag
import campfire.features.filters.ui.generated.resources.filter_category_tracks
import campfire.features.filters.ui.generated.resources.filter_value_missing_asin
import campfire.features.filters.ui.generated.resources.filter_value_missing_authors
import campfire.features.filters.ui.generated.resources.filter_value_missing_description
import campfire.features.filters.ui.generated.resources.filter_value_missing_genres
import campfire.features.filters.ui.generated.resources.filter_value_missing_isbn
import campfire.features.filters.ui.generated.resources.filter_value_missing_language
import campfire.features.filters.ui.generated.resources.filter_value_missing_narrators
import campfire.features.filters.ui.generated.resources.filter_value_missing_publishedYear
import campfire.features.filters.ui.generated.resources.filter_value_missing_publisher
import campfire.features.filters.ui.generated.resources.filter_value_missing_series
import campfire.features.filters.ui.generated.resources.filter_value_missing_subtitle
import campfire.features.filters.ui.generated.resources.filter_value_missing_tags
import campfire.features.filters.ui.generated.resources.filter_value_progress_finished
import campfire.features.filters.ui.generated.resources.filter_value_progress_in_progress
import campfire.features.filters.ui.generated.resources.filter_value_progress_not_finished
import campfire.features.filters.ui.generated.resources.filter_value_progress_not_started
import campfire.features.filters.ui.generated.resources.filter_value_tracks_multi
import campfire.features.filters.ui.generated.resources.filter_value_tracks_single
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
data class ContentFilterUiState(
  val selected: ContentFilter?,
  val filters: ImmutableList<UiItemFilter<*>>,
  val eventSink: (ItemFilterUiEvent) -> Unit,
) : CircuitUiState

sealed interface UiItemFilter<Value : Any> {
  val name: StringResource
  val values: ImmutableList<Value>

  fun isValueSelectedFor(filter: ContentFilter, value: Value): Boolean

  @Composable
  fun valueLabel(value: Value): String

  data class Genres(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_genre

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: ContentFilter, value: String): Boolean {
      return when (filter) {
        is ContentFilter.Genres -> {
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

    override fun isValueSelectedFor(filter: ContentFilter, value: String): Boolean {
      return when (filter) {
        is ContentFilter.Tags -> value == filter.value
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

    override fun isValueSelectedFor(filter: ContentFilter, value: FilterData.Entity): Boolean {
      return when (filter) {
        is ContentFilter.Series -> value.id == filter.value
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

    override fun isValueSelectedFor(filter: ContentFilter, value: FilterData.Entity): Boolean {
      return when (filter) {
        is ContentFilter.Authors -> value.id == filter.value
        else -> false
      }
    }
  }

  data class Progress(
    override val values: ImmutableList<ContentFilter.Progress.Type>,
  ) : UiItemFilter<ContentFilter.Progress.Type> {
    override val name = Res.string.filter_category_progress

    @Composable
    override fun valueLabel(value: ContentFilter.Progress.Type): String {
      return when (value) {
        ContentFilter.Progress.Type.Finished -> stringResource(Res.string.filter_value_progress_finished)
        ContentFilter.Progress.Type.NotStarted -> stringResource(Res.string.filter_value_progress_not_started)
        ContentFilter.Progress.Type.NotFinished -> stringResource(Res.string.filter_value_progress_not_finished)
        ContentFilter.Progress.Type.InProgress -> stringResource(Res.string.filter_value_progress_in_progress)
      }
    }

    override fun isValueSelectedFor(filter: ContentFilter, value: ContentFilter.Progress.Type): Boolean {
      return when (filter) {
        is ContentFilter.Progress -> value.value == filter.value
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

    override fun isValueSelectedFor(filter: ContentFilter, value: String): Boolean {
      return when (filter) {
        is ContentFilter.Narrators -> value == filter.value
        else -> false
      }
    }
  }

  data class Publishers(
    override val values: ImmutableList<String>,
  ) : UiItemFilter<String> {
    override val name = Res.string.filter_category_publisher

    @Composable
    override fun valueLabel(value: String): String = value

    override fun isValueSelectedFor(filter: ContentFilter, value: String): Boolean {
      return when (filter) {
        is ContentFilter.Publishers -> value == filter.value
        else -> false
      }
    }
  }

  data class Missing(
    override val values: ImmutableList<ContentFilter.Missing.Type>,
  ) : UiItemFilter<ContentFilter.Missing.Type> {
    override val name = Res.string.filter_category_missing

    @Composable
    override fun valueLabel(value: ContentFilter.Missing.Type): String {
      return when (value) {
        ContentFilter.Missing.Type.ASIN -> stringResource(Res.string.filter_value_missing_asin)
        ContentFilter.Missing.Type.ISBN -> stringResource(Res.string.filter_value_missing_isbn)
        ContentFilter.Missing.Type.SUBTITLE -> stringResource(Res.string.filter_value_missing_subtitle)
        ContentFilter.Missing.Type.AUTHORS -> stringResource(Res.string.filter_value_missing_authors)
        ContentFilter.Missing.Type.PUBLISHED_YEAR -> stringResource(Res.string.filter_value_missing_publishedYear)
        ContentFilter.Missing.Type.SERIES -> stringResource(Res.string.filter_value_missing_series)
        ContentFilter.Missing.Type.DESCRIPTION -> stringResource(Res.string.filter_value_missing_description)
        ContentFilter.Missing.Type.GENRES -> stringResource(Res.string.filter_value_missing_genres)
        ContentFilter.Missing.Type.TAGS -> stringResource(Res.string.filter_value_missing_tags)
        ContentFilter.Missing.Type.NARRATORS -> stringResource(Res.string.filter_value_missing_narrators)
        ContentFilter.Missing.Type.PUBLISHER -> stringResource(Res.string.filter_value_missing_publisher)
        ContentFilter.Missing.Type.LANGUAGE -> stringResource(Res.string.filter_value_missing_language)
      }
    }

    override fun isValueSelectedFor(filter: ContentFilter, value: ContentFilter.Missing.Type): Boolean {
      return when (filter) {
        is ContentFilter.Missing -> value.value == filter.value
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

    override fun isValueSelectedFor(filter: ContentFilter, value: String): Boolean {
      return when (filter) {
        is ContentFilter.Languages -> value == filter.value
        else -> false
      }
    }
  }

  data class Tracks(
    override val values: ImmutableList<ContentFilter.Tracks.Type>,
  ) : UiItemFilter<ContentFilter.Tracks.Type> {
    override val name = Res.string.filter_category_tracks

    @Composable
    override fun valueLabel(value: ContentFilter.Tracks.Type): String {
      return when (value) {
        ContentFilter.Tracks.Type.Single -> stringResource(Res.string.filter_value_tracks_single)
        ContentFilter.Tracks.Type.Multi -> stringResource(Res.string.filter_value_tracks_multi)
      }
    }

    override fun isValueSelectedFor(
      filter: ContentFilter,
      value: ContentFilter.Tracks.Type,
    ): Boolean = when (filter) {
      is ContentFilter.Tracks -> value.value == filter.value
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
