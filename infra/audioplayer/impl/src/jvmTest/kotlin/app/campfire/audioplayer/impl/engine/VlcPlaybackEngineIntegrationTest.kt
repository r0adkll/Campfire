// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import assertk.assertThat
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Drives the real libvlc engine against a generated WAV file to prove the vlcj integration:
 * non-blocking open, event translation, millisecond start offsets, pause/seek, and end-of-item.
 *
 * Needs VLC 3 installed; when it isn't (CI), the test passes without asserting anything, since
 * the point of the engine abstraction is that everything else stays testable without VLC.
 */
class VlcPlaybackEngineIntegrationTest {

  @Test
  fun `plays a local file from a millisecond offset, pauses, seeks, and reports the end`() {
    val engine = try {
      VlcPlaybackEngine.Factory(listOf("--aout=dummy")).create()
    } catch (e: PlaybackEngineUnavailableException) {
      println("VLC not available, skipping: ${e.message}")
      return
    }

    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val file = File.createTempFile("campfire-tone", ".wav").also { writeTone(it, seconds = 4.0) }
    val item = MediaItem(id = "tone", uri = file.absolutePath, mimeType = "audio/wav")
    val events = Channel<PlaybackEngineEvent>(Channel.UNLIMITED)

    try {
      runBlocking(dispatcher) {
        val collector = engine.events.onEach { events.send(it) }.launchIn(this)
        engine.open(item, startPosition = 1500.milliseconds, playWhenReady = true)
        awaitState(events, EngineState.Playing)
        val firstPosition = awaitPosition(events) { it >= 1.seconds }
        assertThat(firstPosition).isGreaterThanOrEqualTo(1400.milliseconds)
        assertThat(firstPosition).isLessThan(3.seconds)

        engine.pause()
        awaitState(events, EngineState.Paused)

        engine.seekTo(200.milliseconds)
        engine.play()
        awaitState(events, EngineState.Playing)
        val afterSeek = awaitPosition(events) { it < 1.seconds }
        assertThat(afterSeek).isLessThan(1.seconds)

        awaitState(events, EngineState.Ended)
        collector.cancel()
      }
    } finally {
      runBlocking(dispatcher) {
        engine.stop()
        engine.release()
      }
      dispatcher.close()
      file.delete()
    }
  }

  private suspend fun awaitState(events: Channel<PlaybackEngineEvent>, target: EngineState) {
    awaitEvent(events) { it is PlaybackEngineEvent.StateChanged && it.state == target }
  }

  private suspend fun awaitPosition(
    events: Channel<PlaybackEngineEvent>,
    predicate: (Duration) -> Boolean,
  ): Duration {
    val event = awaitEvent(events) { it is PlaybackEngineEvent.PositionChanged && predicate(it.position) }
    return (event as PlaybackEngineEvent.PositionChanged).position
  }

  private suspend fun awaitEvent(
    events: Channel<PlaybackEngineEvent>,
    predicate: (PlaybackEngineEvent) -> Boolean,
  ): PlaybackEngineEvent = withTimeout(TIMEOUT) {
    var event = events.receive()
    while (!predicate(event)) {
      if (event is PlaybackEngineEvent.Error) error("Engine error: ${event.cause}")
      event = events.receive()
    }
    event
  }

  /** A mono 16-bit sine tone; libvlc plays WAV natively so no codec plugin is in play. */
  private fun writeTone(file: File, seconds: Double, hz: Double = 440.0) {
    val rate = 22_050f
    val frames = (seconds * rate).toInt()
    val bytes = ByteArray(frames * 2)
    for (i in 0 until frames) {
      val sample = (sin(2 * PI * hz * i / rate) * 0.2 * Short.MAX_VALUE).toInt()
      bytes[2 * i] = (sample and 0xff).toByte()
      bytes[2 * i + 1] = ((sample shr 8) and 0xff).toByte()
    }
    val format = AudioFormat(rate, 16, 1, true, false)
    AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong()).use { stream ->
      AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file)
    }
  }

  companion object {
    private val TIMEOUT = 15.seconds
  }
}
