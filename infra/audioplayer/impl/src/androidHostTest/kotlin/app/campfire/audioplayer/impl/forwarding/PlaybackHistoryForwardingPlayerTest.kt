// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.common.test.session
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.settings.test.FakePlaybackSettings
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

@UnstableApi
class PlaybackHistoryForwardingPlayerTest {

  private val recorder = RecordingRecorder()
  private val delegate = StubPlayer()
  private val settings = FakePlaybackSettings()
  private var session: Session? = session()
  private val player = PlaybackHistoryForwardingPlayer(
    player = delegate.proxy,
    playbackSettings = settings,
    recorder = recorder,
    session = { session },
  )

  @Test
  fun `play records a Play action`() {
    delegate.position = 5_000

    player.play()

    assertEquals(listOf(Action(PlaybackActionType.Play, 5_000)), recorder.actions)
    assertEquals(session!!.libraryItem.id, recorder.libraryItemIds.single())
  }

  @Test
  fun `starting playback through playWhenReady records a Play action`() {
    player.playWhenReady = true

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `preparing a session without autoplay records nothing`() {
    player.playWhenReady = false

    assertEquals(emptyList(), recorder.types)
  }

  @Test
  fun `re-asserting playWhenReady while already playing records nothing more`() {
    player.play()

    player.playWhenReady = true

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `play while already playing the same book records nothing more`() {
    player.playWhenReady = true

    player.play()

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `play for a new session while playing records a Play for the new item`() {
    player.play()
    val next = session()
    session = next

    player.play()

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Play), recorder.types)
    assertEquals(next.libraryItem.id, recorder.libraryItemIds.last())
  }

  @Test
  fun `play after a pause records a Play`() {
    player.play()
    player.pause()

    player.play()

    assertEquals(
      listOf(PlaybackActionType.Play, PlaybackActionType.Pause, PlaybackActionType.Play),
      recorder.types,
    )
  }

  @Test
  fun `re-preparing the same session while playing records nothing more`() {
    player.playWhenReady = true
    session = session!!.copy(hlsStreamUrl = null)

    player.playWhenReady = true

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `starting a new session while playing records a Play for the new item`() {
    player.playWhenReady = true
    val next = session()
    session = next

    player.playWhenReady = true

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Play), recorder.types)
    assertEquals(next.libraryItem.id, recorder.libraryItemIds.last())
  }

  @Test
  fun `resuming after a pause records a Play`() {
    player.playWhenReady = true
    player.pause()

    player.playWhenReady = true

    assertEquals(
      listOf(PlaybackActionType.Play, PlaybackActionType.Pause, PlaybackActionType.Play),
      recorder.types,
    )
  }

  @Test
  fun `nothing is recorded without a session`() {
    session = null

    player.play()
    player.pause()
    player.playWhenReady = true
    player.seekTo(1_000)
    player.seekForward()

    assertEquals(emptyList(), recorder.types)
  }

  @Test
  fun `pause records a Pause action`() {
    delegate.position = 7_000

    player.pause()

    assertEquals(listOf(Action(PlaybackActionType.Pause, 7_000)), recorder.actions)
  }

  @Test
  fun `seekForward records the configured jump`() {
    settings.forwardTimeMs = 30_000
    delegate.position = 10_000

    player.seekForward()

    assertEquals(listOf(Action(PlaybackActionType.SeekForward, 10_000, 40_000)), recorder.actions)
  }

  @Test
  fun `seekBack records the configured jump`() {
    settings.backwardTimeMs = 10_000
    delegate.position = 25_000

    player.seekBack()

    assertEquals(listOf(Action(PlaybackActionType.SeekBackward, 25_000, 15_000)), recorder.actions)
  }

  @Test
  fun `seekTo records a Seek`() {
    delegate.position = 1_000

    player.seekTo(9_000)

    assertEquals(listOf(Action(PlaybackActionType.Seek, 1_000, 9_000)), recorder.actions)
  }

  @Test
  fun `seekTo an item records a Seek`() {
    delegate.position = 1_000

    player.seekTo(0, 4_000)

    assertEquals(listOf(Action(PlaybackActionType.Seek, 1_000, 4_000)), recorder.actions)
  }

  @Test
  fun `seekTo an item from the start is treated as preparation and records nothing`() {
    player.seekTo(0, 4_000)

    assertEquals(emptyList(), recorder.types)
  }

  @Test
  fun `seekToDefaultPosition records a Seek`() {
    delegate.position = 3_000

    player.seekToDefaultPosition()

    assertEquals(listOf(Action(PlaybackActionType.Seek, 3_000, 0)), recorder.actions)
  }

  @Test
  fun `skipping records SkipNext and SkipPrevious`() {
    delegate.position = 2_000

    player.seekToNext()
    player.seekToNextMediaItem()
    player.seekToPrevious()
    player.seekToPreviousMediaItem()

    assertEquals(
      listOf(
        Action(PlaybackActionType.SkipNext, 2_000, 2_000 + SKIP_MS),
        Action(PlaybackActionType.SkipNext, 2_000 + SKIP_MS, 2_000 + 2 * SKIP_MS),
        Action(PlaybackActionType.SkipPrevious, 2_000 + 2 * SKIP_MS, 2_000 + SKIP_MS),
        Action(PlaybackActionType.SkipPrevious, 2_000 + SKIP_MS, 2_000),
      ),
      recorder.actions,
    )
  }

  private data class Action(
    val type: PlaybackActionType,
    val fromMs: Long,
    val toMs: Long? = null,
  )

  private class RecordingRecorder : PlaybackHistoryRecorder {
    val actions = mutableListOf<Action>()
    val libraryItemIds = mutableListOf<LibraryItemId>()
    val types get() = actions.map { it.type }

    override fun record(
      libraryItemId: LibraryItemId,
      type: PlaybackActionType,
      fromPosition: Duration,
      toPosition: Duration?,
      episodeId: PodcastEpisodeId?,
    ) {
      libraryItemIds += libraryItemId
      actions += Action(type, fromPosition.inWholeMilliseconds, toPosition?.inWholeMilliseconds)
    }
  }

  /**
   * A single-item [Player] that tracks `playWhenReady` and the playback position; skips move the
   * position by [SKIP_MS]. Everything else returns a default.
   */
  private class StubPlayer {
    var playWhenReady = false
    var position = 0L

    val proxy: Player = Proxy.newProxyInstance(
      Player::class.java.classLoader,
      arrayOf(Player::class.java),
    ) { _, method, args ->
      when (method.name) {
        "getPlayWhenReady" -> playWhenReady
        "getCurrentPosition" -> position
        "setPlayWhenReady" -> {
          playWhenReady = args[0] as Boolean
          null
        }
        "play" -> {
          playWhenReady = true
          null
        }
        "pause" -> {
          playWhenReady = false
          null
        }
        "seekTo" -> {
          position = args.last() as Long
          null
        }
        "seekToDefaultPosition" -> {
          position = 0
          null
        }
        "seekToNext", "seekToNextMediaItem" -> {
          position += SKIP_MS
          null
        }
        "seekToPrevious", "seekToPreviousMediaItem" -> {
          position -= SKIP_MS
          null
        }
        else -> when (method.returnType) {
          java.lang.Boolean.TYPE -> false
          java.lang.Integer.TYPE -> 0
          java.lang.Long.TYPE -> 0L
          java.lang.Float.TYPE -> 0f
          else -> null
        }
      }
    } as Player
  }

  private companion object {
    const val SKIP_MS = 60_000L
  }
}
