// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.book

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.libraries.ui.detail.AbstractLibraryItemPresenter
import app.campfire.libraries.ui.detail.ContentUiState
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.SessionUiState
import app.campfire.libraries.ui.detail.composables.slots.AudioTrackSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsTitle
import app.campfire.libraries.ui.detail.composables.slots.CollapsedChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.CoverImageSlot
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesWithBooks
import app.campfire.libraries.ui.detail.composables.slots.SpacerSlot
import app.campfire.libraries.ui.detail.composables.slots.SplitAttributionSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleSlot
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.series.api.SeriesRepository
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.StreamingRoutePredictor
import app.campfire.sessions.api.observeContains
import app.campfire.settings.api.CampfireSettings
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.user.api.MediaProgressRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.by_author_line
import campfire.features.libraries.ui.generated.resources.by_narrator_line
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.tags_title
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.jetbrains.compose.resources.stringResource

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalCoroutinesApi::class)
@Inject
class BookPresenter(
  private val validator: LibraryItemValidator,
  private val seriesRepository: SeriesRepository,
  private val sessionsRepository: SessionsRepository,
  private val sessionQueue: SessionQueue,
  private val streamingRoutePredictor: StreamingRoutePredictor,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackHistoryRepository: PlaybackHistoryRepository,
  private val playbackController: PlaybackController,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
  private val themeManager: ThemeManager,
  private val addToPlaylistDialog: AddToPlaylistDialog,
  private val dispatcherProvider: DispatcherProvider,
) : AbstractLibraryItemPresenter {

  @Composable
  override fun present(
    screen: LibraryItemScreen,
    navigator: Navigator,
    libraryItem: LibraryItem,
  ): ContentUiState {
    val scope = rememberCoroutineScope()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val itemSession by remember {
      derivedStateOf {
        currentSession
          ?.takeIf { it.libraryItem.id == screen.libraryItemId }
          ?.let { SessionUiState.Current(it) }
          ?: SessionUiState.None
      }
    }

    val mediaProgressState by remember {
      mediaProgressRepository.observeProgress(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<MediaProgress?> }
        .catch { emit(LoadState.Error as LoadState<MediaProgress?>) }
    }.collectAsState(LoadState.Loading)

    // Keyed on the series list so the flow rebuilds when the expanded metadata
    // (with the full series list) loads in after the initial minified item.
    val allSeries = libraryItem.media.metadata.series
    val seriesContentState by remember(allSeries) {
      if (allSeries.isEmpty()) {
        flowOf(LoadState.Loaded(emptyList<SeriesWithBooks>()) as LoadState<List<SeriesWithBooks>>)
      } else {
        combine(
          allSeries.map { series ->
            seriesRepository.observeSeriesLibraryItems(series.id)
              .map { SeriesWithBooks(series, it) }
          },
        ) { it.toList() }
          .map { LoadState.Loaded(it) as LoadState<List<SeriesWithBooks>> }
          .catch { emit(LoadState.Error as LoadState<List<SeriesWithBooks>>) }
      }
    }.collectAsState(LoadState.Loading)

    val offlineDownloadState by remember {
      offlineDownloadManager.observeForItem(libraryItem)
    }.collectAsState(null)

    val itemValidation by remember {
      flow { emit(validator.validate(libraryItem)) }
    }.collectAsState(LibraryItemValidation.Success)

    val isPlaying by remember {
      audioPlayerHolder.currentPlayer
        .flatMapLatest {
          if (it?.preparedSession?.libraryItem?.id == screen.libraryItemId) {
            it.state
          } else {
            flowOf(AudioPlayer.State.Disabled)
          }
        }
        .mapLatest { it == AudioPlayer.State.Playing }
        .distinctUntilChanged()
    }.collectAsState(false)

    val playbackSpeed by remember {
      audioPlayerHolder.currentPlayer
        .flatMapLatest {
          if (it?.preparedSession?.libraryItem?.id == screen.libraryItemId) {
            it.playbackSpeed
          } else {
            flowOf(1f)
          }
        }
        .distinctUntilChanged()
    }.collectAsState(1f)

    val isQueued by remember {
      sessionQueue.observeContains(screen.libraryItemId)
    }.collectAsState(false)

    val showConfirmDownloadDialog by remember {
      settings.observeShowConfirmDownload()
    }.collectAsState()

    val showTimeInBook by remember {
      settings.observeShowTimeInBook()
    }.collectAsState()

    var collapseListenedChapters by remember { mutableStateOf(true) }

    val canStreamHls = remember(libraryItem) {
      streamingRoutePredictor.canStreamHls(libraryItem)
    }
    val willStreamHls by remember(libraryItem) {
      streamingRoutePredictor.observeWouldStreamHls(libraryItem)
    }.collectAsState(false)

    val slots = buildSlots(
      libraryItem = libraryItem,
      libraryItemValidation = itemValidation,
      sharedTransitionKey = screen.sharedTransitionKey,
      isPlaying = isPlaying,
      playbackSpeed = playbackSpeed,
      mediaProgressState = mediaProgressState,
      offlineDownloadState = offlineDownloadState,
      seriesContentState = seriesContentState,
      showTimeInBook = showTimeInBook,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
      hasSession = currentSession != null,
      session = itemSession.sessionOrNull(),
      isQueued = isQueued,
      addToPlaylistDialog = addToPlaylistDialog,
      collapseListenedChapters = collapseListenedChapters,
      canStreamHls = canStreamHls,
      willStreamHls = willStreamHls && offlineDownloadState?.isCompleted != true,
    )

    return ContentUiState(
      slots = slots,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.AddToQueue -> {
          if (libraryItem.isEbookOnly) return@ContentUiState
          scope.launch {
            sessionQueue.add(libraryItem)
          }
        }

        LibraryItemUiEvent.RemoveFromQueue -> {
          scope.launch {
            sessionQueue.remove(libraryItem.id)
          }
        }

        is LibraryItemUiEvent.PlayClick -> {
          if (libraryItem.isEbookOnly) return@ContentUiState
          analytics.send(ActionEvent("play_item", Click, extras = event.method?.let { mapOf("method" to it) }))
          playbackController.startSession(libraryItem.id, methodOverride = event.method)
        }

        is LibraryItemUiEvent.AuthorClick -> {
          analytics.send(ActionEvent("author", Click))
          event.item.media.metadata.authors
            .find { it.name == event.author }
            ?.let { author ->
              navigator.goTo(AuthorDetailScreen(author.id, author.name))
            }
        }

        is LibraryItemUiEvent.NarratorClick -> {
          analytics.send(ActionEvent("narrator", Click))
          navigator.goTo(LibraryScreen(ContentFilter.Narrators(event.narrator)))
        }

        is LibraryItemUiEvent.SeriesClick -> {
          analytics.send(ActionEvent("series", Click))
          navigator.goTo(SeriesDetailScreen(event.series.id, event.series.name))
        }

        is LibraryItemUiEvent.DiscardProgress -> {
          analytics.send(ActionEvent("discard_progress", Click))

          // Only stop session if this item is the current playing item
          if (currentSession?.libraryItem?.id == event.item.id) {
            playbackController.stopSession(event.item.id)
          }

          scope.launch {
            sessionsRepository.markDeleted(event.item.id)
            mediaProgressRepository.deleteProgress(event.item.id)
            playbackHistoryRepository.clear(event.item.id)
          }
        }

        is LibraryItemUiEvent.MarkFinished -> {
          analytics.send(ActionEvent("mark_finished", Click))

          // Only stop session if this item is the current playing item
          if (currentSession?.libraryItem?.id == event.item.id) {
            playbackController.stopSession(event.item.id)
          }

          scope.launch {
            sessionsRepository.markDeleted(event.item.id)
            mediaProgressRepository.markFinished(event.item.id)
            playbackHistoryRepository.clear(event.item.id)
          }
        }

        is LibraryItemUiEvent.MarkNotFinished -> {
          analytics.send(ActionEvent("mark_not_finished", Click))
          scope.launch {
            mediaProgressRepository.markNotFinished(event.item.id)
          }
        }

        is LibraryItemUiEvent.ChapterClick -> {
          analytics.send(ActionEvent("chapter", Click))
          val session = itemSession.sessionOrNull()
          val currentPlayer = audioPlayerHolder.currentPlayer.value
          if (event.item.id == session?.libraryItem?.id && currentPlayer != null) {
            // Just seek to the chapter id
            audioPlayerHolder.currentPlayer.value?.seekTo(event.chapter.id)
              ?: throw IllegalStateException("Current session doesn't have a player")
          } else {
            // Start a new session for the item at the given chapter
            playbackController.startSession(event.item.id, true, event.chapter.id)
          }
        }

        is LibraryItemUiEvent.ExpandChaptersClick -> {
          analytics.send(ActionEvent("expand_chapters", Click))
          collapseListenedChapters = !collapseListenedChapters
        }

        is LibraryItemUiEvent.AudioTrackClick -> {
          analytics.send(ActionEvent("track", Click))
          val session = itemSession.sessionOrNull()
          val currentPlayer = audioPlayerHolder.currentPlayer.value
          if (event.item.id == session?.libraryItem?.id && currentPlayer != null) {
            // Just seek to the track index
            audioPlayerHolder.currentPlayer.value?.seekTo(event.track.index - 1)
              ?: throw IllegalStateException("Current session doesn't have a player")
          } else {
            playbackController.startSession(event.item.id, true, event.track.index)
          }
        }

        is LibraryItemUiEvent.DownloadClick -> {
          if (libraryItem.isEbookOnly) return@ContentUiState
          analytics.send(ActionEvent("download", Click))
          settings.showConfirmDownload = !event.doNotShowAgain

          offlineDownloadManager.download(libraryItem)
        }

        LibraryItemUiEvent.RemoveDownloadClick -> {
          analytics.send(ActionEvent("delete_download", Click))

          offlineDownloadManager.delete(libraryItem)
        }

        LibraryItemUiEvent.StopDownloadClick -> {
          analytics.send(ActionEvent("stop_download", Click))
          offlineDownloadManager.stop(libraryItem)
        }

        is LibraryItemUiEvent.TimeInBookChange -> {
          analytics.send(ActionEvent("time_in_book", Click))
          settings.showTimeInBook = event.enabled
        }

        is LibraryItemUiEvent.OpenPlaylist -> {
          analytics.send(ActionEvent("open_playlist", Click))
          navigator.goTo(PlaylistDetailScreen(event.playlistId, null, null, event.isCreated))
        }

        // Drop unhandled events
        else -> Unit
      }
    }
  }
}

@Composable
private fun buildSlots(
  libraryItem: LibraryItem,
  libraryItemValidation: LibraryItemValidation,
  sharedTransitionKey: String,
  isPlaying: Boolean,
  playbackSpeed: Float,
  mediaProgressState: LoadState<out MediaProgress?>,
  offlineDownloadState: OfflineDownload?,
  seriesContentState: LoadState<out List<SeriesWithBooks>>,
  showTimeInBook: Boolean,
  showConfirmDownloadDialog: Boolean,
  hasSession: Boolean,
  isQueued: Boolean,
  session: Session?,
  addToPlaylistDialog: AddToPlaylistDialog,
  collapseListenedChapters: Boolean,
  canStreamHls: Boolean,
  willStreamHls: Boolean,
): List<ContentSlot> {
  return buildList {
    this += CoverImageSlot(
      imageUrl = libraryItem.media.coverImageUrl,
      contentDescription = libraryItem.media.metadata.title,
      sharedTransitionKey = sharedTransitionKey,
    )

    this += TitleSlot(
      libraryItem = libraryItem,
      sharedTransitionKey = sharedTransitionKey,
      showHlsBadge = willStreamHls,
    )

    val authors = libraryItem.media.metadata.authors
      .map { it.name }
      .ifEmpty {
        val authorName = libraryItem.media.metadata.authorName
        if (authorName?.contains(",") == true) {
          authorName.split(",").map { it.trim() }
        } else {
          authorName?.let { listOf(it) }
            ?: emptyList()
        }
      }

    val narrators = libraryItem.media.metadata.narrators
      .ifEmpty {
        val narratorName = libraryItem.media.metadata.narratorName
        if (narratorName?.contains(",") == true) {
          narratorName.split(",").map { it.trim() }
        } else {
          narratorName?.let { listOf(it) }
            ?: emptyList()
        }
      }

    this += SplitAttributionSlot(
      leftLabel = { stringResource(Res.string.by_author_line) },
      leftAttributions = authors,
      rightLabel = { stringResource(Res.string.by_narrator_line) },
      rightAttributions = narrators,
      onLeftAttributeClick = { author ->
        LibraryItemUiEvent.AuthorClick(libraryItem, author)
      },
      onRightAttributeClick = { narrator ->
        LibraryItemUiEvent.NarratorClick(libraryItem, narrator)
      },
    )

    mediaProgressState.onLoaded { mediaProgress ->
      if (mediaProgress != null && mediaProgress.progress > 0f) {
        this += SpacerSlot.xlarge("progress_spacer_before")
        this += ProgressSlot(isPlaying, playbackSpeed, mediaProgress, libraryItem)
        this += SpacerSlot.small("progress_spacer_after")
      }
    }

    this += SpacerSlot.medium("expressive_control_spacer")
    this += ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownloadState,
      mediaProgress = mediaProgressState.dataOrNull,
      isCurrentSession = session != null,
      hasSession = hasSession,
      isQueued = isQueued,
      showConfirmDownloadDialogSetting = showConfirmDownloadDialog,
      addToPlaylistDialog = addToPlaylistDialog,
      canStreamHls = canStreamHls,
      willStreamHls = willStreamHls,
    )

    libraryItem.media.metadata.description?.let { desc ->
      this += SpacerSlot.medium("summary_spacer")
      this += SummarySlot(
        description = desc,
        publisher = libraryItem.media.metadata.publisher,
        publishedYear = libraryItem.media.metadata.publishedYear,
      )
    }

    seriesContentState.onLoaded { seriesWithBooks ->
      val populatedSeries = seriesWithBooks.filter { it.books.isNotEmpty() }
      if (populatedSeries.isNotEmpty()) {
        this += SpacerSlot.medium("series_spacer")
        this += SeriesSlot(
          libraryItem = libraryItem,
          series = populatedSeries,
        )
      }
    }

    libraryItem.media.metadata.genres.takeIf { it.isNotEmpty() }?.let { genres ->
      this += SpacerSlot.medium("genres_spacer")
      this += ChipsSlot(
        title = ChipsTitle(Res.plurals.genres_title, genres.size),
        chips = genres,
      )
    }

    libraryItem.media.tags.takeIf { it.isNotEmpty() }?.let { tags ->
      this += SpacerSlot.medium("tags_spacer")
      this += ChipsSlot(
        title = ChipsTitle(Res.plurals.tags_title, tags.size),
        chips = tags,
      )
    }

    if (libraryItem.media.chapters.isNotEmpty()) {
      this += SpacerSlot.large("chapters_spacer")
      this += ChapterHeaderSlot(
        showTimeInBook = showTimeInBook,
        validation = libraryItemValidation,
      )
      val invalidChapterIds = (libraryItemValidation as? LibraryItemValidation.Error.InvalidChapters)?.chapterIds
      val progress = mediaProgressState.dataOrNull
      var collapsedCount = 0
      libraryItem.media.chapters.forEachIndexed { index, chapter ->
        if (
          collapseListenedChapters &&
          index > 0 &&
          progress != null &&
          !progress.isFinished &&
          progress.currentTime > chapter.end
        ) {
          collapsedCount++
        } else {
          if (
            collapseListenedChapters &&
            collapsedCount > 0 &&
            progress != null &&
            progress.currentTime in chapter.start.rangeUntil(chapter.end)
          ) {
            this += CollapsedChapterSlot(
              numOfCollapsedChapters = collapsedCount,
            )
          }

          this += ChapterSlot(
            libraryItem = libraryItem,
            chapter = chapter,
            showTimeInBook = showTimeInBook,
            mediaProgress = mediaProgressState.dataOrNull,
            isValid = invalidChapterIds?.contains(chapter.id) != true,
          )
        }
      }
    } else if (libraryItem.media.tracks.isNotEmpty()) {
      this += SpacerSlot.large("chapters_spacer")
      this += ChapterHeaderSlot(
        showTimeInBook = showTimeInBook,
        isAudioTracks = true,
      )
      libraryItem.media.tracks.forEach { track ->
        this += AudioTrackSlot(
          libraryItem = libraryItem,
          track = track,
          showTimeInBook = showTimeInBook,
          mediaProgress = mediaProgressState.dataOrNull,
        )
      }
    }
  }
}
