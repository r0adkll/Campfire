package app.campfire.stats.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import app.campfire.core.time.FatherTime
import app.campfire.stats.api.StatsRepository
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.user_stats_recent_sessions_header
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(StatisticsScreen::class, UserScope::class)
@Inject
class StatsPresenter(
  private val statsRepository: StatsRepository,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
  @Assisted private val navigator: Navigator,
) : Presenter<StatsUiState> {

  @Composable
  override fun present(): StatsUiState {
    val libraryStats by remember {
      statsRepository.getLibraryStats()
        .map { LoadState.Loaded(processStats(it)) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    val listeningStats by remember {
      statsRepository.getUserStats()
        .map { LoadState.Loaded(processStats(it)) }
        .catch { LoadState.Error }
    }.collectAsState(LoadState.Loading)

    return StatsUiState(
      libraryStats = libraryStats,
      listeningStats = listeningStats,
    ) { event ->
      when (event) {
        StatsUiEvent.Back -> navigator.pop()
        is StatsUiEvent.ItemClick -> navigator.goTo(LibraryItemScreen(event.itemId))
        is StatsUiEvent.AuthorClick -> navigator.goTo(AuthorDetailScreen(event.authorId, event.authorName))
        is StatsUiEvent.SessionClick -> navigator.goTo(LibraryItemScreen(event.session.libraryItemId))
      }
    }
  }

  private suspend fun processStats(
    stats: ListeningStats,
  ): List<StatsUiModel> = withContext(dispatcherProvider.computation) {
    buildList {
      // Total time
      add(
        StatsUiModel.UserTotals(
          totalDays = stats.days.size,
          totalTime = stats.totalTime,
          days = stats.days.toImmutableMap(),
        ),
      )

      // Items listened to
      add(
        StatsUiModel.ItemsListenedTo(
          items = stats.items
            .sortedByDescending { it.timeListening }
            .toImmutableList(),
        ),
      )

      // Weekly listening
      val today = fatherTime.today()
      val startOfWeek = today - DatePeriod(days = 6)
      val startOfLastWeek = startOfWeek - DatePeriod(days = 6)

      val currentWeekDurations = mutableMapOf<LocalDate, Duration>()
      val lastWeekDurations = mutableMapOf<LocalDate, Duration>()

      stats.days.forEach { (date, duration) ->
        if (date in startOfWeek..today) {
          currentWeekDurations[date] = duration
        } else if (date in startOfLastWeek..startOfWeek) {
          lastWeekDurations[date] = duration
        }
      }

      val weekTime = currentWeekDurations.values
        .fold(Duration.ZERO) { acc, duration -> acc + duration }

      val lastWeekTime = lastWeekDurations.values
        .fold(Duration.ZERO) { acc, duration -> acc + duration }

      val diff = (weekTime - lastWeekTime) / lastWeekTime

      add(
        StatsUiModel.WeeklyListening(
          weekTime = weekTime,
          weekOverWeekChange = diff * 100.0,
          thisWeek = currentWeekDurations.toImmutableMap(),
          lastWeek = lastWeekDurations.toImmutableMap(),
        ),
      )

      // Recent Sessions
      if (stats.recentSessions.isNotEmpty()) {
        add(StatsUiModel.Header(Res.string.user_stats_recent_sessions_header))
        addAll(
          stats.recentSessions
            .sortedByDescending { it.updatedAt }
            .map { StatsUiModel.RecentSession(it) },
        )
      }
    }
  }

  private suspend fun processStats(
    libraryStats: LibraryStats,
  ): List<StatsUiModel> = withContext(dispatcherProvider.computation) {
    buildList {
      add(
        StatsUiModel.LibraryTotals(
          totalItems = libraryStats.totalItems,
          totalSizeInBytes = libraryStats.totalSizeInBytes,
          totalAuthors = libraryStats.totalAuthors,
          totalDuration = libraryStats.totalDuration,
          numAudioTracks = libraryStats.numAudioTracks,
        ),
      )

      add(
        StatsUiModel.LargestItems(
          largestSizeInBytes = libraryStats.largestItems.maxOf { it.sizeInBytes },
          totalSizeInBytes = libraryStats.totalSizeInBytes,
          largestItems = libraryStats.largestItems.toImmutableList(),
        ),
      )

      add(
        StatsUiModel.LongestItems(
          longestDuration = libraryStats.longestItems.maxOf { it.duration },
          totalDuration = libraryStats.totalDuration,
          longestItems = libraryStats.longestItems.toImmutableList(),
        ),
      )

      add(
        StatsUiModel.TopAuthors(
          largestCount = libraryStats.authorsWithCount.maxOf { it.count },
          totalCount = libraryStats.totalAuthors,
          authors = libraryStats.authorsWithCount.toImmutableList(),
        ),
      )
    }
  }
}
