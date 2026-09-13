// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.audioplayer.AudioDevice
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.test.fixtures.chapter
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Session
import app.campfire.sessions.ui.composables.PlaybackSettingsComponent
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import app.campfire.sessions.ui.playback.VolumeUiState
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isGreaterThan
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.rememberOverlayHost
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDateTime
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the desktop bar off-screen in its main states and writes PNGs to `build/renders` for
 * eyeballing; the assertions only guard that each state composes and paints. A hover render
 * sends a pointer over the second chapter's tick so the tooltip path is exercised too.
 */
@OptIn(ExperimentalComposeUiApi::class)
class PlaybackBottomBarRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  init {
    // The skip icons read their seconds from the playback settings component
    ComponentHolder.components += object : PlaybackSettingsComponent {
      override val playbackSettings: PlaybackSettings = FakePlaybackSettings()
    }
  }

  // Two tracks of 30min; three chapters at 0-10, 10-30, 30-60 minutes
  private val book = session(
    chapters = listOf(chapter(0, 0f, 600f), chapter(1, 600f, 1800f), chapter(2, 1800f, 3600f)),
    tracks = listOf(track(1, 0f, 1800f), track(2, 1800f, 1800f)),
    currentTime = 40.minutes,
  )
  private val bookmarks = listOf(
    Bookmark("u", book.libraryItem.id, "Great line", 22.minutes, LocalDateTime(2026, 1, 1, 0, 0)),
    Bookmark("u", book.libraryItem.id, "Plot twist", 51.minutes + 30.seconds, LocalDateTime(2026, 1, 1, 0, 0)),
  )

  @Test
  fun `nothing playing`() {
    render("bottom-bar-empty") {
      Bar(session = null, state = AudioPlayer.State.Disabled, bookTime = Duration.ZERO)
    }
  }

  @Test
  fun `playing a chaptered book with bookmarks`() {
    render("bottom-bar-playing") {
      Bar(session = book, state = AudioPlayer.State.Playing, bookTime = 40.minutes, speed = 1.25f)
    }
  }

  @Test
  fun `showing the app volume control`() {
    render("bottom-bar-volume") {
      Bar(
        session = book,
        state = AudioPlayer.State.Playing,
        bookTime = 40.minutes,
        volume = VolumeUiState(volume = 0.7f, isMuted = false) {},
      )
    }
  }

  @Test
  fun `showing the app volume control muted`() {
    render("bottom-bar-volume-muted") {
      Bar(
        session = book,
        state = AudioPlayer.State.Playing,
        bookTime = 40.minutes,
        volume = VolumeUiState(volume = 0.7f, isMuted = true) {},
      )
    }
  }

  @Test
  fun `the output device picker sits alongside volume when there is room`() {
    render("bottom-bar-devices") {
      Bar(
        session = book,
        state = AudioPlayer.State.Playing,
        bookTime = 40.minutes,
        volume = VolumeUiState(volume = 0.7f, isMuted = false) {},
        outputDevices = OutputDeviceUiState(
          devices = listOf(
            AudioDevice("MacBook Pro Speakers", "MacBook Pro Speakers"),
            AudioDevice("Headphones", "Headphones"),
          ),
          selectedName = "Headphones",
          selectedIsMissing = false,
        ) {},
      )
    }
  }

  @Test
  fun `at the narrowest docked width the tools collapse into an overflow menu`() {
    // 840dp is where this bar takes over from the floating one, and the tool row cannot show
    // everything there — the volume button used to be crushed to nothing.
    render("bottom-bar-narrow", width = NARROW_WIDTH) {
      Bar(
        session = book,
        state = AudioPlayer.State.Playing,
        bookTime = 40.minutes,
        speed = 1.25f,
        volume = VolumeUiState(volume = 0.7f, isMuted = false) {},
      )
    }
  }

  @Test
  fun `hovering a chapter tick shows its tooltip`() {
    render("bottom-bar-hover", beforeRender = { scene ->
      // Track spans [labelWidth + 16, width - labelWidth - 16] in dp; chapter 2 starts at 30/60
      val trackStart = (96 + 8 + 8) * 2f
      val trackEnd = (WIDTH - (96 + 8 + 8)) * 2f
      val x = trackStart + (trackEnd - trackStart) * 0.5f
      scene.sendPointerEvent(PointerEventType.Enter, Offset(x - 40f, 20f * 2f))
      scene.sendPointerEvent(PointerEventType.Move, Offset(x - 20f, 20f * 2f))
      scene.sendPointerEvent(PointerEventType.Move, Offset(x, 20f * 2f))
      println("hover sent at x=$x hasInvalidations=${scene.hasInvalidations()}")
    }) {
      Bar(session = book, state = AudioPlayer.State.Paused, bookTime = 40.minutes)
    }
  }

  @Composable
  private fun Bar(
    session: Session?,
    state: AudioPlayer.State,
    bookTime: Duration,
    speed: Float = 1f,
    volume: VolumeUiState? = null,
    outputDevices: OutputDeviceUiState? = null,
  ) {
    CampfireTheme(useDarkColors = false) {
      ContentWithOverlays(overlayHost = rememberOverlayHost()) {
        Box(Modifier.fillMaxSize()) {
          PlaybackBottomBarContent(
            state = state,
            playbackSpeed = speed,
            currentTime = 10.minutes,
            currentDuration = 30.minutes,
            bookTime = bookTime,
            currentMetadata = Metadata(title = if (session == null) null else "Chapter 3"),
            runningTimer = null,
            equalizer = EqualizerState.Unsupported,
            bookmarks = if (session == null) emptyList() else bookmarks,
            volume = volume,
            outputDevices = outputDevices,
            session = session,
            onPlayPauseClick = {},
            onRewindClick = {},
            onForwardClick = {},
            onSkipNextClick = {},
            onSkipPreviousClick = {},
            onSeekTo = {},
            onTimerSelected = {},
            onTimerCleared = {},
            onChapterSelected = {},
            onAudioTrackSelected = {},
            onBookmarkSelected = {},
          )
        }
      }
    }
  }

  private fun render(
    name: String,
    width: Int = WIDTH,
    beforeRender: (ImageComposeScene) -> Unit = {},
    content: @Composable () -> Unit,
  ) {
    ImageComposeScene(width = width * 2, height = HEIGHT * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      beforeRender(scene)
      val image = scene.render(nanoTime = 1_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertThat(bytes.size).isGreaterThan(1_000)
    }
  }

  private companion object {
    const val WIDTH = 1440
    const val NARROW_WIDTH = 840
    const val HEIGHT = 110
  }
}
