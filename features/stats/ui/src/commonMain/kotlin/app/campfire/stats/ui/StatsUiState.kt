package app.campfire.stats.ui

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.AuthorId
import app.campfire.core.model.AuthorWithCount
import app.campfire.core.model.GenreWithCount
import app.campfire.core.model.ItemListenedTo
import app.campfire.core.model.LargestItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.LongestItem
import app.campfire.core.model.PlaybackSession
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlin.time.Duration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

data class StatsUiState(
  val libraryStats: LoadState<out List<StatsUiModel>>,
  val listeningStats: LoadState<out List<StatsUiModel>>,
  val eventSink: (StatsUiEvent) -> Unit,
) : CircuitUiState

sealed class StatsUiModel(val id: Any) {

  /*
   * Display a header text in the list
   */
  data class Header(
    val title: StringResource,
  ) : StatsUiModel(title.key)

  /*
   * The UI block for displaying user total statistics such as
   * total days or minutes that they've listened to content
   */
  data class UserTotals(
    val totalDays: Int,
    val totalTime: Duration,
    val days: ImmutableMap<LocalDate, Duration>,
  ) : StatsUiModel("Totals")

  /*
   * The UI block for displaying library total statistics such as
   * total items, authors, size, and combined durations
   */
  data class LibraryTotals(
    val totalItems: Int,
    val totalAuthors: Int,
    val totalSizeInBytes: Long,
    val totalDuration: Duration,
    val numAudioTracks: Int,
  ) : StatsUiModel("LibraryTotals")

  /*
   * The UI block for displaying the list/grid of items
   * the user has listened to and their total listening time
   */
  data class ItemsListenedTo(
    val items: ImmutableList<ItemListenedTo>,
  ) : StatsUiModel("ItemsListenedTo")

  /*
   * The UI block for displaying the user's weekly listening
   * stats against their previous week
   */
  data class WeeklyListening(
    val weekTime: Duration,
    val weekOverWeekChange: Double,
    val thisWeek: ImmutableMap<LocalDate, Duration>,
    val lastWeek: ImmutableMap<LocalDate, Duration>,
  ) : StatsUiModel("WeeklyListening")

  /*
   * The UI block for displaying the list of recent listening sessions
   * for the current user
   */
  data class RecentSession(
    val session: PlaybackSession,
  ) : StatsUiModel(session.id)

  data class LargestItems(
    val largestSizeInBytes: Long,
    val totalSizeInBytes: Long,
    val largestItems: ImmutableList<LargestItem>,
  ) : StatsUiModel("LargestItems")

  data class LongestItems(
    val longestDuration: Duration,
    val totalDuration: Duration,
    val longestItems: ImmutableList<LongestItem>,
  ) : StatsUiModel("LongestItems")

  data class TopAuthors(
    val largestCount: Int,
    val totalCount: Int,
    val authors: ImmutableList<AuthorWithCount>,
  ) : StatsUiModel("TopAuthors")

  data class TopGenres(
    val largestCount: Int,
    val totalCount: Int,
    val genres: ImmutableList<GenreWithCount>,
  ) : StatsUiModel("TopGenres")
}

sealed interface StatsUiEvent : CircuitUiEvent {
  data object Back : StatsUiEvent
  data class ItemClick(val itemId: LibraryItemId) : StatsUiEvent
  data class AuthorClick(val authorId: AuthorId, val authorName: String) : StatsUiEvent
  data class SessionClick(val session: PlaybackSession) : StatsUiEvent
}
