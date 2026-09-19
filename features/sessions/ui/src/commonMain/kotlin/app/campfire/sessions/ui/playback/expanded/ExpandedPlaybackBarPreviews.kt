// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import app.campfire.audioplayer.AudioDevice
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.cast.CastButtonComponent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.ui.composables.PlaybackSettingsComponent
import app.campfire.sessions.ui.playback.AvailableSync
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.QueueUiState
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.ThemeUiState
import app.campfire.sessions.ui.playback.VolumeUiState
import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.api.StreamingMethod
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDateTime

/*
 * Previews of every state the expanded playback panel can be in. Each one renders the real
 * ExpandedPlaybackBar (or SmallExpandedPlaybackBar for landscape phones) from a PlaybackUiState,
 * so a state that looks wrong here looks wrong in the app.
 *
 * Not previewable from state: the open queue list and the seek-drag "interacting" play button —
 * both are local UI state inside the panel.
 */

private const val PhoneWidth = 411
private const val PhoneHeight = 891

// region Player states

@Preview(name = "Playing", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarPlayingPreview() = ExpandedPlaybackBarPreview(previewState())

@Preview(name = "Paused", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarPausedPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(state = AudioPlayer.State.Paused) }),
)

@Preview(name = "Buffering", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarBufferingPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(state = AudioPlayer.State.Buffering) }),
)

@Preview(name = "Initializing", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarInitializingPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(state = AudioPlayer.State.Initializing, time = Duration.ZERO) }),
)

@Preview(name = "Finished", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarFinishedPreview() = ExpandedPlaybackBarPreview(
  previewState(
    session = previewBookSession(currentTime = PreviewBookDuration),
    player = {
      copy(
        state = AudioPlayer.State.Finished,
        time = duration,
        bookTime = PreviewBookDuration,
      )
    },
  ),
)

@Preview(name = "Nothing playing", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarNoSessionPreview() = ExpandedPlaybackBarPreview(
  previewState(
    session = null,
    player = {
      copy(
        state = AudioPlayer.State.Disabled,
        metadata = Metadata(),
        time = Duration.ZERO,
        duration = Duration.ZERO,
      )
    },
  ),
)

// endregion

// region Progress display

@Preview(name = "Book time", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarBookTimePreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true) }),
)

@Preview(name = "Book time at 1.5x", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarBookTimeSpedUpPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true, speed = 1.5f) }),
)

@Preview(name = "Book time, unknown duration", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarBookTimeUnknownDurationPreview() = ExpandedPlaybackBarPreview(
  previewState(
    session = previewBookSession(tracks = emptyList()),
    player = { copy(bookTimeEnabled = true) },
  ),
)

@Preview(name = "Wavy slider", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarWavySliderPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(wavySliderEnabled = true) }),
)

// endregion

// region Overlays and banners

@Preview(name = "Sleep timer", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarSleepTimerPreview() = ExpandedPlaybackBarPreview(
  previewState(
    player = {
      copy(
        timer = RunningTimer(
          timer = PlaybackTimer.Epoch(epochMillis = 30.minutes.inWholeMilliseconds),
          startedAt = 0L,
          isShakeToRestartEnabled = false,
          // Frozen so the countdown reads the same on every render.
          pausedAt = 12.minutes.inWholeMilliseconds,
        ),
      )
    },
  ),
)

@Preview(name = "Sleep timer, end of chapter + shake", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarEndOfChapterTimerPreview() = ExpandedPlaybackBarPreview(
  previewState(
    player = {
      copy(
        timer = RunningTimer(
          timer = PlaybackTimer.EndOfChapter(),
          startedAt = 0L,
          isShakeToRestartEnabled = true,
        ),
      )
    },
  ),
)

@Preview(name = "Sync available", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarSyncAvailablePreview() = ExpandedPlaybackBarPreview(
  previewState(
    sync = PreviewAvailableSync.copy(targetChapterTitle = "Chapter 12: The Long Dark"),
  ),
)

@Preview(name = "Sync available, no chapter", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarSyncAvailableNoChapterPreview() = ExpandedPlaybackBarPreview(
  previewState(sync = PreviewAvailableSync),
)

// endregion

// region Errors

@Preview(name = "Misaligned chapters", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarInvalidChaptersPreview() = ExpandedPlaybackBarPreview(
  previewState(validation = LibraryItemValidation.Error.InvalidChapters(setOf(3, 4))),
)

@Preview(name = "Playback error", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarPlaybackErrorPreview() = ExpandedPlaybackBarPreview(
  previewState(
    player = {
      copy(
        state = AudioPlayer.State.Paused,
        error = IllegalStateException("Source error"),
      )
    },
  ),
)

@Preview(name = "Playback engine unavailable", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarEngineUnavailablePreview() = ExpandedPlaybackBarPreview(
  previewState(
    player = {
      copy(
        state = AudioPlayer.State.Disabled,
        error = PlaybackEngineUnavailableException("libvlc not found"),
      )
    },
  ),
)

// endregion

// region Content and platform variants

@Preview(name = "Podcast episode", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarPodcastPreview() = ExpandedPlaybackBarPreview(
  previewState(
    session = previewPodcastSession(),
    player = {
      copy(
        metadata = Metadata(title = PreviewEpisodeTitle),
        time = 18.minutes,
        duration = PreviewEpisodeDuration,
        // Book time is hidden for episodes even when enabled.
        bookTimeEnabled = true,
      )
    },
  ),
)

@Preview(name = "With queue", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarQueuePreview() = ExpandedPlaybackBarPreview(
  previewState(
    queue = listOf(
      QueuedEntry(previewBookSession(title = "Second in Line").libraryItem),
      QueuedEntry(previewBookSession(title = "Third in Line").libraryItem),
    ),
  ),
)

@Preview(name = "Minimal tools (no EQ, no history)", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarMinimalToolsPreview() = ExpandedPlaybackBarPreview(
  previewState(
    playbackHistoryEnabled = false,
    player = { copy(equalizer = EqualizerState.Unsupported) },
  ),
)

@Preview(name = "Desktop tools (volume + output device)", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarDesktopToolsPreview() = ExpandedPlaybackBarPreview(
  previewState(
    volume = VolumeUiState(volume = 0.7f, isMuted = false, eventSink = {}),
    outputDevices = OutputDeviceUiState(
      devices = listOf(
        AudioDevice(id = "speakers", name = "MacBook Pro Speakers"),
        AudioDevice(id = "headphones", name = "Studio Headphones"),
      ),
      selectedName = "Studio Headphones",
      selectedIsMissing = false,
      eventSink = {},
    ),
  ),
)

@Preview(name = "Everything at once", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarKitchenSinkPreview() = ExpandedPlaybackBarPreview(
  previewState(
    validation = LibraryItemValidation.Error.InvalidChapters(setOf(3)),
    sync = PreviewAvailableSync.copy(targetChapterTitle = "Chapter 12: The Long Dark"),
    queue = listOf(QueuedEntry(previewBookSession(title = "Second in Line").libraryItem)),
    player = {
      copy(
        bookTimeEnabled = true,
        speed = 1.25f,
        wavySliderEnabled = true,
        error = IllegalStateException("Source error"),
        timer = RunningTimer(
          timer = PlaybackTimer.Epoch(epochMillis = 30.minutes.inWholeMilliseconds),
          startedAt = 0L,
          isShakeToRestartEnabled = true,
          pausedAt = 12.minutes.inWholeMilliseconds,
        ),
      )
    },
  ),
)

@Preview(name = "Long titles", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarLongTitlesPreview() = ExpandedPlaybackBarPreview(
  previewState(
    session = previewBookSession(
      title = "The Extraordinarily Long and Winding Tale of a Book Whose Title Never Seems to End",
    ),
    player = {
      copy(
        metadata = Metadata(
          title = "Chapter Forty-Two: In Which Our Heroes Finally Reach the Place They Set Out For",
        ),
      )
    },
  ),
)

// endregion

// region Theme and window size

@Preview(name = "Light theme", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ExpandedPlaybackBarLightPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true) }),
  darkTheme = false,
)

@Preview(name = "Small phone", widthDp = 360, heightDp = 640)
@Composable
private fun ExpandedPlaybackBarSmallPhonePreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true) }),
  windowSizeClass = WindowSizeClass(minWidthDp = 360, minHeightDp = 640),
)

@Preview(name = "Tablet (supporting pane)", widthDp = 480, heightDp = 1000)
@Composable
private fun ExpandedPlaybackBarTabletPreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true) }),
  // The panel lives in the supporting pane, so it is narrower than the medium-width window.
  windowSizeClass = WindowSizeClass(minWidthDp = 840, minHeightDp = 1000),
)

@Preview(name = "Landscape phone", widthDp = PhoneHeight, heightDp = PhoneWidth)
@Composable
private fun SmallExpandedPlaybackBarLandscapePreview() = ExpandedPlaybackBarPreview(
  previewState(player = { copy(bookTimeEnabled = true) }),
  windowSizeClass = WindowSizeClass(minWidthDp = PhoneHeight, minHeightDp = PhoneWidth),
)

// endregion

/**
 * Hosts the panel the way `CampfirePlaybackBar` does — inside a shared-transition layout and a
 * visible animated scope — choosing the landscape-phone variant from [windowSizeClass] as the
 * app does.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalCoilApi::class)
@Composable
private fun ExpandedPlaybackBarPreview(
  state: PlaybackUiState,
  darkTheme: Boolean = true,
  windowSizeClass: WindowSizeClass = WindowSizeClass(minWidthDp = PhoneWidth, minHeightDp = PhoneHeight),
) {
  installPreviewComponents()

  CompositionLocalProvider(
    LocalWindowSizeClass provides windowSizeClass,
    // Stands in for the cover art, which never loads in a preview.
    LocalAsyncImagePreviewHandler provides PreviewCoverHandler,
  ) {
    CampfireTheme(useDarkColors = darkTheme) {
      SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
          val overlayHost = rememberOverlayHost()
          if (windowSizeClass.isLandscapePhone) {
            SmallExpandedPlaybackBar(
              navigator = Navigator.NoOp,
              overlayHost = overlayHost,
              session = state.session,
              itemValidation = state.validation,
              playerState = state.playerState,
              queueState = state.queueState,
              syncState = state.syncUiState,
              playbackHistoryEnabled = state.playbackHistoryEnabled,
              onClose = {},
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            ExpandedPlaybackBar(
              navigator = Navigator.NoOp,
              overlayHost = overlayHost,
              session = state.session,
              itemValidation = state.validation,
              playerState = state.playerState,
              queueState = state.queueState,
              syncState = state.syncUiState,
              playbackHistoryEnabled = state.playbackHistoryEnabled,
              volumeState = state.volume,
              outputDeviceState = state.outputDevices,
              onClose = {},
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
  }
}

// region Fixtures

@OptIn(ExperimentalCoilApi::class)
private val PreviewCoverHandler = AsyncImagePreviewHandler { ColorImage(0xFFB5651D.toInt()) }

private val PreviewBookDuration = 9.hours + 42.minutes
private val PreviewEpisodeDuration = 58.minutes
private const val PreviewEpisodeTitle = "Episode 214: Campfire Stories"

private val PreviewAvailableSync = AvailableSync(
  itemId = "preview-item",
  currentTime = 3.hours + 12.minutes,
  targetTime = 4.hours + 5.minutes,
  syncTimeInMillis = Clock.System.now().toEpochMilliseconds() - 2.hours.inWholeMilliseconds,
)

private fun previewState(
  session: Session? = previewBookSession(),
  validation: LibraryItemValidation = LibraryItemValidation.Success,
  sync: AvailableSync? = null,
  queue: List<QueuedEntry> = emptyList(),
  playbackHistoryEnabled: Boolean = true,
  volume: VolumeUiState? = null,
  outputDevices: OutputDeviceUiState? = null,
  player: PlayerUiState.() -> PlayerUiState = { this },
) = PlaybackUiState(
  session = session,
  playerState = PlayerUiState(
    time = 14.minutes + 32.seconds,
    bookTime = 3.hours + 12.minutes,
    bookTimeEnabled = false,
    duration = 38.minutes,
    wavySliderEnabled = false,
    metadata = Metadata(title = "Chapter 7: Embers"),
    state = AudioPlayer.State.Playing,
    speed = 1f,
    equalizer = EqualizerState.Available(EqualizerProfile()),
    timer = null,
    bookmarks = emptyList(),
    error = null,
    eventSink = {},
  ).player(),
  queueState = QueueUiState(queue = queue, eventSink = {}),
  syncUiState = SyncUiState(mediaProgress = null, availableSync = sync, eventSink = {}),
  themeState = ThemeUiState(dynamicThemingEnabled = false, theme = null),
  validation = validation,
  playbackHistoryEnabled = playbackHistoryEnabled,
  volume = volume,
  outputDevices = outputDevices,
  eventSink = {},
)

private fun previewBookSession(
  title: String = "The Long Way Home",
  currentTime: Duration = 3.hours + 12.minutes,
  tracks: List<AudioTrack> = listOf(previewTrack(PreviewBookDuration)),
): Session {
  val chapters = List(15) { index ->
    val length = PreviewBookDuration.inWholeSeconds.toFloat() / 15
    Chapter(id = index, start = index * length, end = (index + 1) * length, title = "Chapter ${index + 1}")
  }
  val media = Media.Book(
    id = "preview-media",
    metadata = Media.Metadata.Book(
      title = title,
      titleIgnorePrefix = title,
      subtitle = null,
      authorName = "Ada Ember",
      authorNameLastFirst = "Ember, Ada",
      narratorName = "Sam Hearth",
      seriesName = null,
      series = emptyList(),
      genres = emptyList(),
      publishedYear = null,
      publishedDate = null,
      publisher = null,
      description = null,
      ISBN = null,
      ASIN = null,
      language = null,
      isExplicit = false,
      isAbridged = false,
    ),
    coverImageUrl = "https://example.com/cover.jpg",
    coverPath = null,
    tags = emptyList(),
    numTracks = tracks.size,
    numAudioFiles = tracks.size,
    numChapters = chapters.size,
    numMissingParts = 0,
    numInvalidAudioFiles = 0,
    durationInMillis = tracks.sumOf { it.duration.toDouble() * 1000 }.toLong(),
    sizeInBytes = 0L,
    chapters = chapters,
    tracks = tracks,
  )
  return previewSession(previewLibraryItem("preview-item", MediaType.Book, media), currentTime)
}

private fun previewPodcastSession(): Session {
  val episode = PodcastEpisode(
    id = "preview-episode",
    libraryItemId = "preview-podcast",
    podcastId = "preview-podcast-media",
    title = PreviewEpisodeTitle,
    description = "Stories told around the fire.",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = PreviewEpisodeDuration.inWholeMilliseconds,
    sizeInBytes = 0L,
    audioTrack = previewTrack(PreviewEpisodeDuration),
  )
  val media = Media.Podcast(
    id = "preview-podcast-media",
    metadata = Media.Metadata.Podcast(
      title = "The Campfire Hour",
      author = "Ada Ember",
      description = null,
      releaseDate = null,
      genres = emptyList(),
      feedUrl = null,
      imageUrl = null,
      itunesPageUrl = null,
      itunesId = null,
      itunesArtistId = null,
      isExplicit = false,
      language = null,
      podcastType = null,
    ),
    coverImageUrl = "https://example.com/podcast.jpg",
    coverPath = null,
    tags = emptyList(),
    sizeInBytes = 0L,
    episodes = listOf(episode),
  )
  return previewSession(previewLibraryItem("preview-podcast", MediaType.Podcast, media), 18.minutes)
    .copy(episodeId = episode.id)
}

private fun previewTrack(duration: Duration) = AudioTrack(
  index = 1,
  startOffset = 0f,
  duration = duration.inWholeSeconds.toFloat(),
  title = "Track 1",
  contentUrl = "/track1.m4b",
  mimeType = "audio/mp4",
  codec = "aac",
  metadata = FileMetadata("track1.m4b", ".m4b", "/track1.m4b", "track1.m4b", 0, 0, 0, 0),
  metaTags = null,
)

private fun previewLibraryItem(
  id: LibraryItemId,
  mediaType: MediaType,
  media: Media,
) = LibraryItem(
  id = id,
  ino = "ino",
  libraryId = "preview-library",
  folderId = "preview-folder",
  path = "/preview",
  relPath = "preview",
  isFile = false,
  mtimeMs = 0L,
  ctimeMs = 0L,
  birthtimeMs = 0L,
  isMissing = false,
  isInvalid = false,
  mediaType = mediaType,
  numFiles = 1,
  sizeInBytes = 0L,
  addedAtMillis = 0L,
  updatedAtMillis = 0L,
  media = media,
)

private fun previewSession(item: LibraryItem, currentTime: Duration): Session {
  val now = LocalDateTime(2026, 1, 1, 0, 0)
  return Session(
    id = Uuid.NIL,
    libraryItem = item,
    userId = "preview-user",
    isDeleted = false,
    playMethod = PlayMethod.DirectPlay,
    mediaPlayer = "campfire",
    timeListening = Duration.ZERO,
    startTime = Duration.ZERO,
    currentTime = currentTime,
    lastPlayedAt = null,
    startedAt = now,
    updatedAt = now,
  )
}

// endregion

// region Preview-only DI

/**
 * The skip icons and the top bar's cast button pull their dependencies from [ComponentHolder],
 * which is empty in a preview. Registers stand-ins once per preview process.
 */
internal fun installPreviewComponents() {
  if (ComponentHolder.maybeComponent<PlaybackSettingsComponent>() == null) {
    ComponentHolder.components += object : PlaybackSettingsComponent {
      override val playbackSettings: PlaybackSettings = PreviewPlaybackSettings()
    }
  }
  if (ComponentHolder.maybeComponent<CastButtonComponent>() == null) {
    ComponentHolder.components += object : CastButtonComponent {
      override val castController: CastController = object : CastController {
        override val state: StateFlow<CastState> = MutableStateFlow(CastState.NotConnected)
        override val availableDevices: StateFlow<List<CastDevice>> = MutableStateFlow(emptyList())
        override fun connect(device: CastDevice) = Unit
      }
    }
  }
}

/** Only the skip intervals are read by the panel; everything else just satisfies the interface. */
internal class PreviewPlaybackSettings : PlaybackSettings {
  override var enableMp3IndexSeeking = false
  override fun observeMp3IndexSeeking(): StateFlow<Boolean> = MutableStateFlow(enableMp3IndexSeeking)
  override var forwardTimeMs = 30_000L
  override var backwardTimeMs = 10_000L
  override fun observeForwardTimeMs(): StateFlow<Long> = MutableStateFlow(forwardTimeMs)
  override fun observeBackwardTimeMs(): StateFlow<Long> = MutableStateFlow(backwardTimeMs)
  override var trackResetThreshold: Duration = 5.seconds
  override fun observeTrackResetThreshold(): StateFlow<Duration> = MutableStateFlow(trackResetThreshold)
  override var playbackRates = listOf(1f, 1.25f, 1.5f, 2f)
  override fun observePlaybackRates(): StateFlow<List<Float>> = MutableStateFlow(playbackRates)
  override var playbackSpeed = 1f
  override var itemPlaybackSpeeds = emptyMap<LibraryItemId, Float>()
  override fun observeItemPlaybackSpeeds(): StateFlow<Map<LibraryItemId, Float>> = MutableStateFlow(itemPlaybackSpeeds)
  override var remoteNextPrevSkipsChapters = true
  override fun observeRemoteNextPrevSkipsChapters(): StateFlow<Boolean> = MutableStateFlow(remoteNextPrevSkipsChapters)
  override var syncEnabled = true
  override fun observeSyncEnabled(): StateFlow<Boolean> = MutableStateFlow(syncEnabled)
  override var autoSyncEnabled = false
  override fun observeAutoSyncEnabled(): StateFlow<Boolean> = MutableStateFlow(autoSyncEnabled)
  override var playbackHistoryEnabled = true
  override fun observePlaybackHistoryEnabled(): StateFlow<Boolean> = MutableStateFlow(playbackHistoryEnabled)
  override var syncIntervalUnmetered: Duration = 15.seconds
  override fun observeSyncIntervalUnmetered(): StateFlow<Duration> = MutableStateFlow(syncIntervalUnmetered)
  override var syncIntervalMetered: Duration = 60.seconds
  override fun observeSyncIntervalMetered(): StateFlow<Duration> = MutableStateFlow(syncIntervalMetered)
  override var streamingMethod = StreamingMethod.DIRECT_PLAY_ONLY
  override fun observeStreamingMethod(): StateFlow<StreamingMethod> = MutableStateFlow(streamingMethod)
  override var autoRewindOnResumeEnabled = false
  override fun observeAutoRewindOnResumeEnabled(): StateFlow<Boolean> = MutableStateFlow(autoRewindOnResumeEnabled)
  override var resumeRewindConfig = ResumeRewindConfig.Default
  override fun observeResumeRewindConfig(): StateFlow<ResumeRewindConfig> = MutableStateFlow(resumeRewindConfig)
  override var autoRewindStopAtChapterBoundary = true
  override fun observeAutoRewindStopAtChapterBoundary(): StateFlow<Boolean> =
    MutableStateFlow(autoRewindStopAtChapterBoundary)
  override var pendingResumeRewind: PendingResumeRewind? = null
  override var bookTimeInPlaybackUi = false
  override fun observeBookTimeInPlaybackUi(): StateFlow<Boolean> = MutableStateFlow(bookTimeInPlaybackUi)
  override var playbackWavyScrubber = false
  override fun observePlaybackWavyScrubber(): StateFlow<Boolean> = MutableStateFlow(playbackWavyScrubber)
}

// endregion
