// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.StatisticsScreen
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.ListeningStats
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.stats.api.StatsRepository
import app.campfire.user.api.MediaProgressRepository
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.user_stats_activity_header
import campfire.features.stats.ui.generated.resources.user_stats_recent_sessions_header
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(StatisticsScreen::class, UserScope::class)
@Inject
class StatsPresenter(
  private val statsRepository: StatsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
  @Assisted private val navigator: Navigator,
) : Presenter<StatsUiState> {

  @Composable
  override fun present(): StatsUiState {
    val libraryStats by remember {
      statsRepository.getLibraryStats()
        .map { LoadState.Loaded(processStats(it)) }
        .catch<LoadState<out List<StatsUiModel>>> { emit(LoadState.Error) }
    }.collectAsState(LoadState.Loading)

    val listeningStats by remember {
      combine(
        statsRepository.getUserStats(),
        // Start with an empty list so a progress store that never emits can't
        // hold the whole stats screen in its loading state
        mediaProgressRepository.observeAllProgress()
          .onStart { emit(emptyList()) },
      ) { stats, progress ->
        LoadState.Loaded(processStats(stats, progress))
      }.catch<LoadState<out List<StatsUiModel>>> { emit(LoadState.Error) }
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
    allProgress: List<MediaProgress>,
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

      // Activity
      add(StatsUiModel.Header(Res.string.user_stats_activity_header))
      add(computeActivity(stats, allProgress))

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

      // Listening heatmap
      add(
        StatsUiModel.ListeningHeatmap(
          days = stats.days.toImmutableMap(),
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

  private fun computeActivity(
    stats: ListeningStats,
    allProgress: List<MediaProgress>,
  ): StatsUiModel.Activity {
    val today = fatherTime.today()
    val listenedDates = stats.days
      .filterValues { it > Duration.ZERO }
      .keys

    // Longest run of consecutive listening days
    var bestStreak = 0
    var run = 0
    var previous: LocalDate? = null
    for (date in listenedDates.sorted()) {
      run = if (previous?.daysUntil(date) == 1) run + 1 else 1
      bestStreak = maxOf(bestStreak, run)
      previous = date
    }

    // Current streak, anchored on today or yesterday so an unfinished
    // day doesn't read as a broken streak
    val anchor = when {
      today in listenedDates -> today
      (today - DatePeriod(days = 1)) in listenedDates -> today - DatePeriod(days = 1)
      else -> null
    }
    var currentStreak = 0
    var cursor = anchor
    while (cursor != null && cursor in listenedDates) {
      currentStreak++
      cursor -= DatePeriod(days = 1)
    }

    val listenedDurations = stats.days.values.filter { it > Duration.ZERO }
    val dailyAverage = if (listenedDurations.isEmpty()) {
      Duration.ZERO
    } else {
      stats.totalTime / listenedDurations.size
    }

    val timeZone = TimeZone.currentSystemDefault()
    fun MediaProgress.finishedYear(): Int? = finishedAt
      ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date.year }

    val finished = allProgress.filter { it.isFinished }
    val finishedBooks = finished.filter { it.episodeId == null && it.mediaItemType == MediaType.Book }
    val finishedEpisodes = finished.filter { it.episodeId != null }

    return StatsUiModel.Activity(
      currentStreak = currentStreak,
      bestStreak = bestStreak,
      dailyAverage = dailyAverage,
      bestDay = listenedDurations.maxOrNull() ?: Duration.ZERO,
      booksFinished = finishedBooks.size,
      booksFinishedThisYear = finishedBooks.count { it.finishedYear() == today.year },
      hasPodcastActivity = allProgress.any { it.episodeId != null },
      episodesFinished = finishedEpisodes.size,
      episodesFinishedThisYear = finishedEpisodes.count { it.finishedYear() == today.year },
    )
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

      if (libraryStats.authorsWithCount != null && libraryStats.totalAuthors != null) {
        add(
          StatsUiModel.TopAuthors(
            largestCount = libraryStats.authorsWithCount!!.maxOf { it.count },
            totalCount = libraryStats.totalAuthors!!,
            authors = libraryStats.authorsWithCount!!.toImmutableList(),
          ),
        )
      }
    }
  }
}
