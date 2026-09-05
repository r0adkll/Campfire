// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.engine.ffmpeg.FfmpegPlaybackEngine
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
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Drives each real engine against a generated WAV file to prove the native integration behind
 * [PlaybackEngine]: non-blocking open, event translation, millisecond start offsets, pause/seek,
 * end-of-item, and (FFmpeg) tempo and equalizer filtering.
 *
 * libvlc needs VLC 3 installed; without it that case passes without asserting (CI). FFmpeg ships
 * its own natives and always runs.
 */
class PlaybackEngineIntegrationTest {

  private fun vlc(): PlaybackEngine? = try {
    VlcPlaybackEngine.Factory(listOf("--aout=dummy")).create()
  } catch (e: PlaybackEngineUnavailableException) {
    println("VLC not available, skipping: ${e.message}")
    null
  }

  @Test
  fun `libvlc plays a local file from a millisecond offset, pauses, seeks, and reports the end`() {
    val engine = vlc() ?: return
    exercise(engine)
  }

  @Test
  fun `ffmpeg plays a local file from a millisecond offset, pauses, seeks, and reports the end`() {
    exercise(FfmpegPlaybackEngine())
  }

  @Test
  fun `ffmpeg plays through the tempo and equalizer graph, finishing faster at 2x`() {
    val engine = FfmpegPlaybackEngine()
    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val file = File.createTempFile("campfire-tone", ".wav").also { writeTone(it, seconds = 4.0) }
    val item = MediaItem(id = "tone", uri = file.absolutePath, mimeType = "audio/wav")
    val events = Channel<PlaybackEngineEvent>(Channel.UNLIMITED)
    try {
      runBlocking(dispatcher) {
        val collector = engine.events.onEach { events.send(it) }.launchIn(this)
        engine.volume = 0f
        engine.setRate(2f)
        engine.setEqualizer(enabled = true, preampDb = 3f, bandGainsDb = List(10) { if (it < 2) 6f else -2f })

        val started = System.nanoTime()
        engine.open(item, startPosition = Duration.ZERO, playWhenReady = true)
        awaitState(events, EngineState.Playing)
        val position = awaitPosition(events) { it >= 2.seconds }
        awaitState(events, EngineState.Ended)
        val elapsed = (System.nanoTime() - started).nanoseconds

        // 4s of audio at 2x plays in ~2s (+ line buffer); positions still count media time
        assertThat(elapsed).isLessThan(3200.milliseconds)
        assertThat(position).isGreaterThanOrEqualTo(2.seconds)
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

  private fun exercise(engine: PlaybackEngine) {
    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val file = File.createTempFile("campfire-tone", ".wav").also { writeTone(it, seconds = 4.0) }
    val item = MediaItem(id = "tone", uri = file.absolutePath, mimeType = "audio/wav")
    val events = Channel<PlaybackEngineEvent>(Channel.UNLIMITED)

    try {
      runBlocking(dispatcher) {
        val collector = engine.events.onEach { events.send(it) }.launchIn(this)
        engine.volume = 0f
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
