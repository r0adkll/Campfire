package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.common.screens.AuthorDetailScreen
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.map
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.api.LibraryItemFilter
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.api.screen.LibraryScreen
import app.campfire.libraries.ui.detail.composables.slots.AudioTrackSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsTitle
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.CoverImageSlot
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SpacerSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleAndAuthorSlot
import app.campfire.series.api.SeriesRepository
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.CampfireSettings
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.SwatchSelector
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.user.api.MediaProgressRepository
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.tags_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.r0adkll.swatchbuckler.compose.Schema
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Inject
class LibraryItemPresenter(
  @Assisted private val screen: LibraryItemScreen,
  @Assisted private val navigator: Navigator,
  private val repository: LibraryItemRepository,
  private val seriesRepository: SeriesRepository,
  private val sessionsRepository: SessionsRepository,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackController: PlaybackController,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val settings: CampfireSettings,
  private val analytics: Analytics,
  private val themeManager: ThemeManager,
  private val themeSettings: ThemeSettings,
  private val dispatcherProvider: DispatcherProvider,
) : Presenter<LibraryItemUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): LibraryItemUiState {
    val scope = rememberCoroutineScope()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
        .filterNotNull()
        .filter { it.libraryItem.id == screen.libraryItemId }
        .map { SessionUiState.Current(it) }
    }.collectAsState(SessionUiState.None)

    val libraryItemContentState by remember {
      repository.observeLibraryItem(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<LibraryItem> }
        .catch { emit(LoadState.Error as LoadState<LibraryItem>) }
    }.collectAsState(LoadState.Loading)

    val mediaProgressState by remember {
      mediaProgressRepository.observeProgress(screen.libraryItemId)
        .map { LoadState.Loaded(it) as LoadState<MediaProgress?> }
        .catch { emit(LoadState.Error as LoadState<MediaProgress?>) }
    }.collectAsState(LoadState.Loading)

    val seriesContentState by remember {
      snapshotFlow {
        libraryItemContentState.dataOrNull
          ?.media?.metadata?.seriesSequence
      }
        .filterNotNull()
        .flatMapLatest { seriesSequence ->
          seriesRepository.observeSeriesLibraryItems(seriesSequence.id)
            .map { LoadState.Loaded(it) as LoadState<List<LibraryItem>> }
            .catch { emit(LoadState.Error as LoadState<List<LibraryItem>>) }
        }
    }.collectAsState(LoadState.Loading)

    val offlineDownloadState by remember {
      snapshotFlow { libraryItemContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { libraryItem ->
          offlineDownloadManager.observeForItem(libraryItem)
        }
    }.collectAsState(null)

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

    val showConfirmDownloadDialog by remember {
      settings.observeShowConfirmDownload()
    }.collectAsState(true)

    val showTimeInBook by remember {
      settings.observeShowTimeInBook()
    }.collectAsState(true)

    val isDynamicThemingEnabled by remember {
      themeSettings.observeDynamicallyThemeItemDetail()
    }.collectAsState()

    val theme by remember(isDynamicThemingEnabled) {
      if (!isDynamicThemingEnabled) {
        flowOf(null)
      } else themeManager.observeThemeFor(
        key = screen.libraryItemId,
        colorSelector = SwatchSelector.Dominant,
        schema = Schema.Expressive,
      )
    }.collectAsState(null)

    // Build the Slots
    val slots = libraryItemContentState.map { libraryItem ->
      buildSlots(
        libraryItem = libraryItem,
        sharedTransitionKey = screen.sharedTransitionKey,
        isPlaying = isPlaying,
        isDynamicThemingEnabled = isDynamicThemingEnabled,
        mediaProgressState = mediaProgressState,
        offlineDownloadState = offlineDownloadState,
        seriesContentState = seriesContentState,
        showTimeInBook = showTimeInBook,
        showConfirmDownloadDialog = showConfirmDownloadDialog,
      )
    }

    return LibraryItemUiState(
      libraryItem = libraryItemContentState.dataOrNull,
      theme = theme,
      contentState = slots,
      showConfirmDownloadDialog = showConfirmDownloadDialog,
    ) { event ->
      when (event) {
        LibraryItemUiEvent.OnBack -> navigator.pop()

        is LibraryItemUiEvent.SeedColorChange -> {
          scope.launch(dispatcherProvider.computation) {
            themeManager.enqueue(
              key = screen.libraryItemId,
              seedColor = event.seedColor,
            )
          }
        }

        is LibraryItemUiEvent.PlayClick -> {
          analytics.send(ActionEvent("play_item", Click))
          playbackController.startSession(event.item.id)
        }

        is LibraryItemUiEvent.AuthorClick -> {
          analytics.send(ActionEvent("author", Click))
          event.item.media.metadata.authors.firstOrNull()?.let { author ->
            navigator.goTo(AuthorDetailScreen(author.id, author.name))
          }
        }

        is LibraryItemUiEvent.NarratorClick -> {
          analytics.send(ActionEvent("narrator", Click))
          event.item.media.metadata.narratorName?.let { narrator ->
            navigator.goTo(LibraryScreen(LibraryItemFilter.Narrators(narrator)))
          }
        }

        is LibraryItemUiEvent.SeriesClick -> {
          analytics.send(ActionEvent("series", Click))
          val series = event.item.media.metadata.seriesSequence ?: return@LibraryItemUiState
          navigator.goTo(SeriesDetailScreen(series.id, series.name))
        }

        is LibraryItemUiEvent.DiscardProgress -> {
          analytics.send(ActionEvent("discard_progress", Click))
          playbackController.stopSession(event.item.id)
          scope.launch {
            sessionsRepository.deleteSession(event.item.id)
            mediaProgressRepository.deleteProgress(event.item.id)
          }
        }

        is LibraryItemUiEvent.MarkFinished -> {
          analytics.send(ActionEvent("mark_finished", Click))
          playbackController.stopSession(event.item.id)
          scope.launch {
            sessionsRepository.deleteSession(event.item.id)
            mediaProgressRepository.markFinished(event.item.id)
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
          val session = currentSession.sessionOrNull()
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

        is LibraryItemUiEvent.AudioTrackClick -> {
          analytics.send(ActionEvent("track", Click))
          val session = currentSession.sessionOrNull()
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
          analytics.send(ActionEvent("download", Click))
          settings.showConfirmDownload = !event.doNotShowAgain

          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.download(it)
          }
        }

        LibraryItemUiEvent.RemoveDownloadClick -> {
          analytics.send(ActionEvent("delete_download", Click))

          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.delete(it)
          }
        }

        LibraryItemUiEvent.StopDownloadClick -> {
          analytics.send(ActionEvent("stop_download", Click))
          libraryItemContentState.dataOrNull?.let {
            offlineDownloadManager.stop(it)
          }
        }

        is LibraryItemUiEvent.TimeInBookChange -> {
          analytics.send(ActionEvent("time_in_book", Click))
          settings.showTimeInBook = event.enabled
        }
      }
    }
  }
}

@Composable
private fun buildSlots(
  libraryItem: LibraryItem,
  sharedTransitionKey: String,
  isPlaying: Boolean,
  isDynamicThemingEnabled: Boolean,
  mediaProgressState: LoadState<out MediaProgress?>,
  offlineDownloadState: OfflineDownload?,
  seriesContentState: LoadState<out List<LibraryItem>>,
  showTimeInBook: Boolean,
  showConfirmDownloadDialog: Boolean,
): List<ContentSlot> {
  return buildList {
    this += CoverImageSlot(
      imageUrl = libraryItem.media.coverImageUrl,
      contentDescription = libraryItem.media.metadata.title,
      sharedTransitionKey = sharedTransitionKey,
      isDynamicThemingEnabled = isDynamicThemingEnabled,
    )

    this += TitleAndAuthorSlot(
      libraryItem = libraryItem,
    )

    mediaProgressState.onLoaded { mediaProgress ->
      if (mediaProgress != null && mediaProgress.progress > 0f) {
        this += SpacerSlot.xlarge("progress_spacer_before")
        this += ProgressSlot(isPlaying, mediaProgress, libraryItem)
        this += SpacerSlot.small("progress_spacer_after")
      }
    }

    this += SpacerSlot.medium("expressive_control_spacer")
    this += ExpressiveControlSlot(
      libraryItem = libraryItem,
      offlineDownload = offlineDownloadState,
      mediaProgress = mediaProgressState.dataOrNull,
      showConfirmDownloadDialogSetting = showConfirmDownloadDialog,
    )

    libraryItem.media.metadata.description?.let { desc ->
      this += SpacerSlot.medium("summary_spacer")
      this += SummarySlot(
        description = desc,
        publisher = libraryItem.media.metadata.publisher,
        publishedYear = libraryItem.media.metadata.publishedYear,
      )
    }

    seriesContentState.onLoaded { seriesBooks ->
      if (seriesBooks.isNotEmpty()) {
        this += SpacerSlot.medium("series_spacer")
        this += SeriesSlot(
          libraryItem = libraryItem,
          seriesBooks = seriesBooks,
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
      )
      libraryItem.media.chapters.forEach { chapter ->
        this += ChapterSlot(
          libraryItem = libraryItem,
          chapter = chapter,
          showTimeInBook = showTimeInBook,
          mediaProgress = mediaProgressState.dataOrNull,
        )
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
