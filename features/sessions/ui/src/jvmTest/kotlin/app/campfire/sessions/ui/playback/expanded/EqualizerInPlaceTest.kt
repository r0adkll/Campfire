// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.fixtures.chapter
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import app.campfire.audioplayer.ui.cast.CastButtonComponent
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.ComponentHolder
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.ui.composables.PlaybackSettingsComponent
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.QueueUiState
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.sheets.equalizer.EqualizerBottomSheetComponent
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage

/**
 * On a region laid out sideways the equalizer takes the cover and transport's place in the body
 * rather than opening over them — ten vertical band sliders want height, and a panel down the edge
 * of a short region has less of it than the region does.
 *
 * The point of the swap is that the tool column stays put, so the button that opened the equalizer
 * is still on screen to close it. That is what these drive: the button, not the geometry.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class EqualizerInPlaceTest {

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
    ComponentHolder.components += object : EqualizerBottomSheetComponent {
      override val equalizerSettings: EqualizerSettings = FakeEqualizerSettings()
      override val audioPlayerHolder: AudioPlayerHolder = FakeAudioPlayerHolder()
    }
  }

  @Test
  fun `the equalizer button swaps the body for the equalizer and back`() = runSkikoComposeUiTest(
    size = Size(914f, 411f),
    density = Density(1f),
  ) {
    // The player animates while it is playing, so the clock never goes idle on its own. Drive it
    // by hand and step past each swap instead.
    mainClock.autoAdvance = false
    setContent { LandscapePlayer() }
    settle()

    onNodeWithText(BookTitle).assertIsDisplayed()

    onNodeWithContentDescription(EqualizerLabel).performClick()
    settle()

    // The equalizer replaced the cover and transport...
    onNodeWithContentDescription(PerBookToggle).assertIsDisplayed()
    // ...and its own button is still there beside it, which is how it closes again.
    onNodeWithContentDescription(EqualizerLabel).assertIsDisplayed()

    onNodeWithContentDescription(EqualizerLabel).performClick()
    settle()

    onNodeWithText(BookTitle).assertIsDisplayed()
  }

  @Test
  fun `the equalizer in place, for eyeballing`() = runLandscapePhone {
    mainClock.autoAdvance = false
    setContent { LandscapePlayer() }
    settle()

    onNodeWithContentDescription(EqualizerLabel).performClick()
    settle()

    val image = SkiaImage.makeFromBitmap(onAllNodes(isRoot())[0].captureToImage().asSkiaBitmap())
    File(renders, "small-expanded-equalizer.png")
      .writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
  }

  /** A Pixel 8 held sideways, at the size the real window reports. */
  private fun runLandscapePhone(body: ComposeUiTest.() -> Unit) =
    runSkikoComposeUiTest(size = Size(914f, 411f), density = Density(1f)) { body() }

  /** Steps the clock far enough for a content swap to finish, without waiting for idleness. */
  private fun ComposeUiTest.settle() {
    repeat(10) { mainClock.advanceTimeBy(100) }
  }

  @androidx.compose.runtime.Composable
  private fun LandscapePlayer() {
    // A Pixel 8 held sideways: wide and short, so the player lays out with its tool column.
    val sizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(914f, 411f)

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
                bookTimeEnabled = true,
                duration = 30.minutes,
                wavySliderEnabled = false,
                metadata = Metadata(title = BookTitle),
                state = AudioPlayer.State.Playing,
                speed = 1.25f,
                // Available, so the tool column actually offers the equalizer.
                equalizer = EqualizerState.Available(EqualizerProfile()),
                timer = null,
                bookmarks = emptyList(),
                error = null,
                eventSink = {},
              ),
              queueState = QueueUiState(queue = emptyList(), eventSink = {}),
              syncState = SyncUiState(mediaProgress = null, availableSync = null, eventSink = {}),
              playbackHistoryEnabled = true,
              volumeState = null,
              outputDeviceState = null,
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

  private val book = session(
    chapters = listOf(chapter(0, 0f, 600f), chapter(1, 600f, 1800f)),
    tracks = listOf(track(1, 0f, 1800f)),
    currentTime = 40.minutes,
  )

  private companion object {
    const val BookTitle = "Chapter 3"
    const val EqualizerLabel = "Equalizer"
    const val PerBookToggle = "Use a separate equalizer for this book"
  }
}
