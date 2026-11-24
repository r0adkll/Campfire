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

    prepare(session!!.copy(hlsStreamUrl = null), playImmediately = true)

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
    player.play()
    delegate.position = 7_000

    player.pause()

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 7_000)),
      recorder.actions,
    )
  }

  @Test
  fun `pausing while already paused records nothing more`() {
    player.play()
    player.pause()

    player.pause()
    player.playWhenReady = false

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `pausing before anything played records nothing`() {
    player.pause()

    assertEquals(emptyList(), recorder.types)
  }

  @Test
  fun `pausing without recording records nothing, now or when the book later stops`() {
    player.play()

    player.withoutRecording { player.pause() }
    player.stop()
    prepare(session(), playImmediately = false)

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `seeking without recording records nothing, and recording resumes after`() {
    delegate.position = 1_000

    player.withoutRecording {
      player.seekTo(0, 9_000)
      player.seekTo(4_000)
    }
    player.seekTo(6_000)

    assertEquals(listOf(Action(PlaybackActionType.Seek, 4_000, 6_000)), recorder.actions)
  }

  @Test
  fun `playing after pausing without recording records a Play`() {
    player.play()
    player.withoutRecording { player.pause() }

    player.play()

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Play), recorder.types)
  }

  /**
   * The MediaSession (notification, lock screen, Bluetooth, Auto) drives the chapter-window
   * player, a ForwardingSimpleBasePlayer, which forwards pause() to this layer as
   * setPlayWhenReady(false) rather than pause().
   */
  @Test
  fun `pausing through playWhenReady while playing records a Pause action`() {
    player.play()
    delegate.position = 7_000

    player.playWhenReady = false

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 7_000)),
      recorder.actions,
    )
  }

  @Test
  fun `switching books with autoplay while playing records a Pause for the old book`() {
    val old = session!!
    player.play()
    delegate.position = 9_000
    val next = session()

    prepare(next, playImmediately = true)

    assertEquals(
      listOf(
        Action(PlaybackActionType.Play, 0),
        Action(PlaybackActionType.Pause, 9_000),
        Action(PlaybackActionType.Play, 0),
      ),
      recorder.actions,
    )
    assertEquals(listOf(old.libraryItem.id, old.libraryItem.id, next.libraryItem.id), recorder.libraryItemIds)
  }

  @Test
  fun `switching books without autoplay while playing records a Pause for the old book`() {
    val old = session!!
    player.play()
    delegate.position = 9_000

    prepare(session(), playImmediately = false)

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 9_000)),
      recorder.actions,
    )
    assertEquals(listOf(old.libraryItem.id, old.libraryItem.id), recorder.libraryItemIds)
  }

  @Test
  fun `switching books while paused records nothing more`() {
    player.play()
    player.pause()

    prepare(session(), playImmediately = false)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `stopping while playing records a Pause`() {
    player.play()
    delegate.position = 4_000

    player.stop()

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 4_000)),
      recorder.actions,
    )
  }

  @Test
  fun `stopping while paused records nothing more`() {
    player.play()
    player.pause()

    player.stop()

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `replacing the queue any other way while playing records a Pause for the old book`() {
    val replacements = listOf<(Player) -> Unit>(
      { it.setMediaItems(emptyList()) },
      { it.setMediaItems(emptyList(), 0, 0L) },
      { it.clearMediaItems() },
    )
    replacements.forEach { replace ->
      recorder.actions.clear()
      session = session()
      player.play()
      delegate.position = 3_000
      session = session()

      replace(player)

      assertEquals(
        listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 3_000)),
        recorder.actions,
      )
      player.pause()
    }
  }

  @Test
  fun `clearing the queue while the same book plays records a Pause`() {
    player.play()
    delegate.position = 4_000

    player.clearMediaItems()

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 4_000)),
      recorder.actions,
    )
  }

  @Test
  fun `headphones disconnecting while playing records a Pause`() {
    player.play()
    delegate.position = 6_000

    delegate.pauseItself(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)

    assertEquals(
      listOf(Action(PlaybackActionType.Play, 0), Action(PlaybackActionType.Pause, 6_000)),
      recorder.actions,
    )
  }

  @Test
  fun `losing audio focus while playing records a Pause`() {
    player.play()

    delegate.pauseItself(Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `the player pausing itself for any other reason records a Pause`() {
    val reasons = listOf(
      Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM,
      Player.PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG,
    )
    reasons.forEach { reason ->
      recorder.actions.clear()
      player.play()

      delegate.pauseItself(reason)

      assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
    }
  }

  @Test
  fun `the player reporting a requested pause records nothing more`() {
    player.play()
    player.playWhenReady = false

    delegate.pauseItself(Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `a book ending then advancing to the next records no Pause for the finished book`() {
    player.play()
    val next = session()

    delegate.end()
    prepare(next, playImmediately = true)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Play), recorder.types)
    assertEquals(next.libraryItem.id, recorder.libraryItemIds.last())
  }

  @Test
  fun `a book ending then stopping records no Pause for the finished book`() {
    player.play()

    delegate.end()
    player.stop()

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `a pause from the cast receiver records a Pause`() {
    player.play()

    delegate.pauseItself(Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `playback failing while playing records a Pause, and playing once re-prepared records a Play`() {
    player.play()
    delegate.position = 8_000

    player.recordPlaybackFailed()
    player.prepare()
    player.play()

    assertEquals(
      listOf(
        Action(PlaybackActionType.Play, 0),
        Action(PlaybackActionType.Pause, 8_000),
        Action(PlaybackActionType.Play, 8_000),
      ),
      recorder.actions,
    )
  }

  @Test
  fun `playing after a failure without re-preparing records nothing, since nothing plays`() {
    player.play()

    player.recordPlaybackFailed()
    player.play()
    player.playWhenReady = true

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `starting another book after a failure records a Play for it`() {
    player.play()
    player.recordPlaybackFailed()
    val next = session()

    prepare(next, playImmediately = true)

    assertEquals(
      listOf(PlaybackActionType.Play, PlaybackActionType.Pause, PlaybackActionType.Play),
      recorder.types,
    )
    assertEquals(next.libraryItem.id, recorder.libraryItemIds.last())
  }

  @Test
  fun `playing a book again from its start after the end records a Play`() {
    player.play()
    delegate.position = 50_000
    delegate.end()
    player.pause()

    // The media controls' play at the end: seek to the start, then play
    player.seekToDefaultPosition()
    player.play()

    assertEquals(
      listOf(PlaybackActionType.Play, PlaybackActionType.Seek, PlaybackActionType.Play),
      recorder.types,
    )
  }

  @Test
  fun `tapping a chapter at the end of a book resumes it with a Play, and its pause is recorded`() {
    player.play()
    delegate.position = 50_000
    delegate.end()

    // ExoPlayerAudioPlayer.seekTo(itemIndex): the chapter's start, then play
    player.seekToDefaultPosition(3)
    player.play()
    player.pause()

    assertEquals(
      listOf(PlaybackActionType.Play, PlaybackActionType.Play, PlaybackActionType.Pause),
      recorder.types,
    )
  }

  @Test
  fun `tapping a chapter mid-book still records no Seek`() {
    player.play()
    delegate.position = 5_000

    player.seekToDefaultPosition(2)

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `skipping next on the last chapter at the end records no Play, since nothing plays`() {
    player.play()
    delegate.position = 50_000
    delegate.end()
    delegate.atLastItem = true

    player.seekToNext()
    player.stop()

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.SkipNext), recorder.types)
  }

  @Test
  fun `playback failing while paused records nothing more`() {
    player.play()
    player.pause()

    player.recordPlaybackFailed()

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Pause), recorder.types)
  }

  @Test
  fun `seeking out of the end of a book resumes it with a Play, even while the player still reports the end`() {
    player.play()
    delegate.position = 50_000
    delegate.end()

    player.seekTo(20_000)
    player.pause()

    assertEquals(
      listOf(
        Action(PlaybackActionType.Play, 0),
        Action(PlaybackActionType.Seek, 50_000, 20_000),
        Action(PlaybackActionType.Play, 20_000),
        Action(PlaybackActionType.Pause, 20_000),
      ),
      recorder.actions,
    )
  }

  @Test
  fun `play at the end of a book records nothing, since nothing plays`() {
    player.play()
    delegate.end()

    player.play()

    assertEquals(listOf(PlaybackActionType.Play), recorder.types)
  }

  @Test
  fun `seeking out of the end after pausing there records only the Seek`() {
    player.play()
    delegate.end()
    player.pause()

    player.seekTo(20_000)

    assertEquals(listOf(PlaybackActionType.Play, PlaybackActionType.Seek), recorder.types)
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

  /** Mirrors ExoPlayerAudioPlayer.prepare: swap the session, replace the queue, then autoplay. */
  private fun prepare(next: Session, playImmediately: Boolean) {
    session = next
    player.setMediaItems(emptyList(), true)
    player.playWhenReady = playImmediately
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
    var playbackState = Player.STATE_READY

    /** On the last item, skipping next is ignored, as Media3's BasePlayer does. */
    var atLastItem = false
    private val listeners = mutableListOf<Player.Listener>()

    /** A pause the player makes on its own, e.g. headphones unplugged, reported to listeners. */
    fun pauseItself(reason: Int) {
      playWhenReady = false
      listeners.forEach { it.onPlayWhenReadyChanged(false, reason) }
    }

    /**
     * The queue playing out to its end; `playWhenReady` stays set, as in ExoPlayer. It keeps
     * reporting the end through later seeks, like a cast player that hasn't caught up.
     */
    fun end() {
      playbackState = Player.STATE_ENDED
      listeners.forEach { it.onPlaybackStateChanged(Player.STATE_ENDED) }
    }

    val proxy: Player = Proxy.newProxyInstance(
      Player::class.java.classLoader,
      arrayOf(Player::class.java),
    ) { _, method, args ->
      when (method.name) {
        "getPlayWhenReady" -> playWhenReady
        "getCurrentPosition" -> position
        "getPlaybackState" -> playbackState
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
        "addListener" -> {
          listeners += args[0] as Player.Listener
          null
        }
        "setMediaItems", "clearMediaItems" -> {
          position = 0
          null
        }
        "seekToDefaultPosition" -> {
          position = 0
          null
        }
        "seekToNext", "seekToNextMediaItem" -> {
          if (!atLastItem) position += SKIP_MS
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
