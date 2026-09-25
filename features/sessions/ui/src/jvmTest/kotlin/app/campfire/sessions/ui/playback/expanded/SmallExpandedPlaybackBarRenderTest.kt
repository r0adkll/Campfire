// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.audioplayer.AudioDevice
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
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.di.ComponentHolder
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.ui.composables.PlaybackSettingsComponent
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.QueueUiState
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.VolumeUiState
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.EncodedImageFormat

/**
 * The landscape player at the widths phones actually report, including the ones below the Expanded
 * breakpoint that used to fall through to the portrait arrangement (see #1133). PNGs land in
 * `build/renders` for eyeballing; the assertions only guard that each size picks this layout and
 * paints.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalSharedTransitionApi::class)
class SmallExpandedPlaybackBarRenderTest {

  private val renders = File("build/renders").apply { mkdirs() }

  init {
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
  fun `a Galaxy S24 sideways gets the landscape layout`() {
    render("small-expanded-780", width = 780, height = 360)
  }

  @Test
  fun `a Galaxy S24+ at its default resolution gets it too`() {
    render("small-expanded-832", width = 832, height = 384)
  }

  @Test
  fun `a Pixel 8 sideways is unchanged`() {
    render("small-expanded-914", width = 914, height = 411)
  }

  /**
   * The readout used to run off the bottom edge on the shortest of these windows. Rendering the
   * same size with the readout disabled at the source gives the picture the layout should now
   * produce on its own: identical where it has to drop the readout, different where it keeps it.
   */
  @Test
  fun `the shortest windows drop the whole-book readout instead of clipping it`() {
    assertThat(bytes(width = 780, height = 360))
      .isEqualTo(bytes(width = 780, height = 360, bookTime = false))
  }

  @Test
  fun `a window with the height for it keeps the whole-book readout`() {
    assertThat(bytes(width = 780, height = 384))
      .isNotEqualTo(bytes(width = 780, height = 384, bookTime = false))
  }

  @Test
  fun `a short desktop window fills the volume and output-device slots`() {
    render(
      "small-expanded-desktop",
      width = 900,
      height = 420,
      volumeState = VolumeUiState(volume = 0.6f, isMuted = false, eventSink = {}),
      outputDeviceState = OutputDeviceUiState(
        devices = listOf(AudioDevice("MacBook Pro Speakers", "MacBook Pro Speakers")),
        selectedName = null,
        selectedIsMissing = false,
        eventSink = {},
      ),
    )
  }

  private fun render(
    name: String,
    width: Int,
    height: Int,
    volumeState: VolumeUiState? = null,
    outputDeviceState: OutputDeviceUiState? = null,
    bookTime: Boolean = true,
  ) {
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())
    assertThat(sizeClass.isLandscapePhone).isTrue()

    val bytes = bytes(width, height, volumeState, outputDeviceState, bookTime)
    File(renders, "$name.png").writeBytes(bytes)
    println("rendered ${File(renders, "$name.png").absolutePath}")
    assertThat(bytes.size).isGreaterThan(1_000)
  }

  private fun bytes(
    width: Int,
    height: Int,
    volumeState: VolumeUiState? = null,
    outputDeviceState: OutputDeviceUiState? = null,
    bookTime: Boolean = true,
  ): ByteArray {
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2
      .computeWindowSizeClass(width.toFloat(), height.toFloat())

    return ImageComposeScene(
      width = width * 2,
      height = height * 2,
      density = Density(2f),
      content = { Bar(sizeClass, volumeState, outputDeviceState, bookTime) },
    ).use { scene ->
      scene.render()
      scene.render(nanoTime = 1_000_000_000L)
        .encodeToData(EncodedImageFormat.PNG)!!
        .bytes
    }
  }

  @Composable
  private fun Bar(
    sizeClass: WindowSizeClass,
    volumeState: VolumeUiState?,
    outputDeviceState: OutputDeviceUiState?,
    bookTime: Boolean = true,
  ) {
    CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
      CampfireTheme(useDarkColors = false) {
        SharedTransitionLayout {
          AnimatedVisibility(visible = true) {
            SmallExpandedPlaybackBar(
              navigator = Navigator.NoOp,
              overlayHost = rememberOverlayHost(),
              session = book,
              itemValidation = LibraryItemValidation.Success,
              playerState = PlayerUiState(
                time = 10.minutes,
                bookTime = 40.minutes,
                bookTimeEnabled = bookTime,
                duration = 30.minutes,
                wavySliderEnabled = false,
                metadata = Metadata(title = "Chapter 3"),
                state = AudioPlayer.State.Playing,
                speed = 1.25f,
                equalizer = EqualizerState.Unsupported,
                timer = null,
                bookmarks = emptyList(),
                error = null,
                eventSink = {},
              ),
              queueState = QueueUiState(queue = emptyList(), eventSink = {}),
              syncState = SyncUiState(mediaProgress = null, availableSync = null, eventSink = {}),
              playbackHistoryEnabled = true,
              volumeState = volumeState,
              outputDeviceState = outputDeviceState,
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
