// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Round-trips Now Playing metadata and transport state through the real MediaPlayer.framework and
 * registers remote-command targets, proving the Objective-C bridge (message sends, runtime class
 * definition, main-queue dispatch, framework globals) works on this machine.
 *
 * macOS only, and needs a window server for the AppKit run loop; skips itself elsewhere.
 */
class MacNowPlayingBridgeIntegrationTest {

  @Test
  fun `publishes now playing info and registers remote commands`() {
    if (!System.getProperty("os.name").orEmpty().lowercase().contains("mac") || GraphicsEnvironment.isHeadless()) {
      println("Not a macOS desktop session, skipping")
      return
    }
    // Starts the AppKit run loop on the main thread, which the main-queue dispatch relies on
    Toolkit.getDefaultToolkit()

    val bridge = MacNowPlayingBridge()
    val commands = object : RemoteCommandHandler {
      override fun play() = Unit
      override fun pause() = Unit
      override fun togglePlayPause() = Unit
      override fun skipForward() = Unit
      override fun skipBackward() = Unit
      override fun nextTrack() = Unit
      override fun previousTrack() = Unit
      override fun seekTo(position: kotlin.time.Duration) = Unit
    }

    try {
      bridge.setCommandHandler(commands, skipForward = 30.seconds, skipBackward = 10.seconds)
      bridge.setNowPlaying(
        NowPlayingInfo(
          title = "Campfire bridge test",
          artist = "An Author",
          album = "A Book",
          duration = 10.minutes,
          elapsed = 42.seconds,
          rate = 1.0,
          defaultRate = 1.0,
          artwork = pngCover(),
        ),
      )
      bridge.setPlaybackState(NowPlayingState.Playing)

      val (title, state, artworkResolves) = bridge.readBack()
      assertThat(title).isEqualTo("Campfire bridge test")
      assertThat(state).isEqualTo(1L)
      assertThat(artworkResolves).isTrue()
    } finally {
      bridge.setCommandHandler(null, 0.seconds, 0.seconds)
      bridge.setNowPlaying(null)
      bridge.setPlaybackState(NowPlayingState.Stopped)
    }

    val (title, state, artworkResolves) = bridge.readBack()
    assertThat(title).isNull()
    assertThat(state).isEqualTo(3L)
    assertThat(artworkResolves).isFalse()
  }

  /** A small solid PNG, encoded the way a real cover download would arrive: as bytes. */
  private fun pngCover(): ByteArray {
    val image = BufferedImage(64, 96, BufferedImage.TYPE_INT_RGB)
    image.createGraphics().apply {
      color = Color(0xCC, 0x55, 0x22)
      fillRect(0, 0, 64, 96)
      dispose()
    }
    return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
  }
}
