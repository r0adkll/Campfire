// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.test.fixtures.chapter
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.audioplayer.ui.cast.CastButtonComponent
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.ui.composables.PlaybackSettingsComponent
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.QueueUiState
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.ThemeUiState
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isGreaterThan
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders the dedicated player off-screen at the two sizes it is built for — a mini-player
 * window and the lower half of a half-open foldable — and writes PNGs to `build/renders` for
 * eyeballing. The assertions only guard that each composes and paints.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DedicatedPlayerRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  init {
    // The skip icons read their seconds from the playback settings component, and the top bar's
    // cast button needs a controller to ask about devices.
    ComponentHolder.components += object : PlaybackSettingsComponent {
      override val playbackSettings: PlaybackSettings = FakePlaybackSettings()
    }
    ComponentHolder.components += object : CastButtonComponent {
      override val castController: CastController = object : CastController {
        override val state: StateFlow<CastState> = MutableStateFlow(CastState.Unavailable)
        override val availableDevices: StateFlow<List<CastDevice>> = MutableStateFlow(emptyList())
        override fun connect(device: CastDevice) = Unit
      }
    }
  }

  private val book = session(
    chapters = listOf(chapter(0, 0f, 600f), chapter(1, 600f, 1800f), chapter(2, 1800f, 3600f)),
    tracks = listOf(track(1, 0f, 1800f), track(2, 1800f, 1800f)),
    currentTime = 40.minutes,
  )

  @Test
  fun `a mini-player window stacks the cover over the controls`() {
    render("dedicated-player-tall", width = 380, height = 640) {
      Player(session = book)
    }
  }

  @Test
  fun `a roomy foldable lower half grows the transport beside the tools column`() {
    render("dedicated-player-wide", width = 900, height = 430) {
      Player(session = book)
    }
  }

  @Test
  fun `the tightest foldable lower half still fits the large transport and the readout`() {
    render("dedicated-player-wide-tight", width = 840, height = 340) {
      Player(session = book)
    }
  }

  @Test
  fun `a wide mini-player window puts its chrome in a leading rail`() {
    render("dedicated-player-wide-rail", width = 600, height = 380) {
      Player(session = book, wideChrome = WideChromePlacement.LeadingRail)
    }
  }

  @Test
  fun `a square mini-player window shows just the transport over the cover`() {
    render("dedicated-player-compact", width = 360, height = 360) {
      Player(session = book)
    }
  }

  @Test
  fun `nothing playing keeps the surface and top bar in place`() {
    render("dedicated-player-empty", width = 380, height = 640) {
      Player(session = null)
    }
  }

  @Composable
  private fun Player(
    session: Session?,
    wideChrome: WideChromePlacement = WideChromePlacement.TopBar,
  ) {
    CampfireTheme(useDarkColors = false) {
      DedicatedPlayerContent(
        uiState = PlaybackUiState(
          session = session,
          playerState = PlayerUiState(
            time = 10.minutes,
            bookTime = 40.minutes,
            bookTimeEnabled = true,
            duration = 30.minutes,
            wavySliderEnabled = false,
            metadata = Metadata(title = if (session == null) null else "Chapter 3"),
            state = AudioPlayer.State.Playing,
            speed = 1.25f,
            equalizer = EqualizerState.Unsupported,
            timer = null,
            bookmarks = emptyList(),
            error = null,
            eventSink = {},
          ),
          queueState = QueueUiState(queue = emptyList(), eventSink = {}),
          syncUiState = SyncUiState(mediaProgress = null, availableSync = null, eventSink = {}),
          themeState = ThemeUiState(dynamicThemingEnabled = false, theme = null),
          validation = LibraryItemValidation.Success,
          playbackHistoryEnabled = true,
          volume = null,
          outputDevices = null,
          eventSink = {},
        ),
        onItemClick = {},
        navigationIcon = { MiniPlayerAction(isOpen = true, onClick = {}) },
        wideChrome = wideChrome,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }

  private fun render(
    name: String,
    width: Int,
    height: Int,
    content: @Composable () -> Unit,
  ) {
    ImageComposeScene(width = width * 2, height = height * 2, density = Density(2f), content = content).use { scene ->
      scene.render()
      val image = scene.render(nanoTime = 1_000_000_000L)
      val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
      File(renders, "$name.png").writeBytes(bytes)
      println("rendered ${File(renders, "$name.png").absolutePath}")
      assertThat(bytes.size).isGreaterThan(1_000)
    }
  }
}
