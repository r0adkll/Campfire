// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Round-trips Now Playing metadata, cover art, and transport state through the real
 * MediaPlayer.framework and registers remote-command targets, proving the Objective-C bridge
 * (message sends, runtime class definition, block literal, main-queue dispatch, framework
 * globals) works on this machine.
 *
 * Opt-in: run with `CAMPFIRE_MACOS_INTEGRATION=1` on a macOS desktop session. MediaPlayer talks
 * to the media daemon asynchronously and a bare test JVM (no NSApplication) has crashed inside
 * the framework on exit, so it stays out of the default suite; the coordinator and policy tests
 * cover the logic, and the running app is the real check.
 */
class MacNowPlayingBridgeIntegrationTest {

  @Test
  fun `publishes now playing info and registers remote commands`() {
    if (System.getenv("CAMPFIRE_MACOS_INTEGRATION").isNullOrBlank() ||
      !System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    ) {
      println("Set CAMPFIRE_MACOS_INTEGRATION=1 on macOS to run the MediaPlayer round trip, skipping")
      return
    }
    // The java launcher already runs a CFRunLoop on the main thread, which services the main
    // dispatch queue the bridge posts to; AppKit is deliberately not initialised here

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

    // MediaPlayer pushes Now Playing changes to the system asynchronously on its own queue and
    // retries when the media daemon isn't ready; a test JVM that exits mid-push crashes inside
    // the framework's dealloc. Let the last push settle while the process is still alive.
    Thread.sleep(PUSH_SETTLE_MILLIS)
  }

  private companion object {
    const val PUSH_SETTLE_MILLIS = 1_500L
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
