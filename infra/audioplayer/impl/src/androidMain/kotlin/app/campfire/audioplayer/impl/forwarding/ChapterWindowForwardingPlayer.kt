// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.settings.api.PlaybackSettings
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Presents coarse single-item (HLS) playback to MediaController consumers — the system media
 * notification, Android Auto, Bluetooth/car controllers — as a virtual playlist of chapter
 * windows, so their scrubbers, time labels, titles, and next/previous buttons carry chapter
 * semantics identical to the chapter-granular local queue. The in-app UI derives the same
 * view from [app.campfire.audioplayer.impl.chapters.ChapterTimeline]; this is the same
 * projection applied at the session boundary.
 *
 * When [Host.activeChapters] returns null (chapter queues, per-track cast, chapterless
 * items) the player is fully transparent: state and commands pass straight through to the
 * wrapped chain, preserving [RemoteControlForwardingPlayer]'s next/prev behavior.
 *
 * Chapter crossings mid-item produce no wrapped-player event, so the audio player pokes
 * [onChapterProgress] from its progress tick to re-derive state.
 */
@UnstableApi
class ChapterWindowForwardingPlayer(
  player: Player,
  private val settings: PlaybackSettings,
  private val appPackageName: String,
  private val host: Host,
) : ForwardingSimpleBasePlayer(player) {

  interface Host {
    /** The active chapter table, or null whenever chapter windowing shouldn't apply. */
    fun activeChapters(): List<Chapter>?

    /** Seeks the absolute session timeline without forcing playback (boundary-marker safe). */
    fun seekToAbsolute(target: Duration)

    /** Chapter-skip with the same semantics as the in-app controls. */
    fun skipToNextChapter()

    /** Chapter-skip honoring the track-reset threshold, like the in-app controls. */
    fun skipToPreviousChapter()
  }

  /** Set after session creation, to identify the controller a command came from. */
  var session: MediaSession? = null

  private val wrapped: Player = player

  private var cachedChapters: List<Chapter>? = null
  private var cachedPlaylist: List<MediaItemData> = emptyList()

  /** Re-derives state; called from the audio player's progress tick while windowing is active. */
  fun onChapterProgress() {
    invalidateState()
  }

  override fun getState(): State {
    val state = super.getState()
    val chapters = host.activeChapters() ?: return state
    if (chapters.isEmpty() || state.playlist.isEmpty()) return state

    val positionMs = wrapped.currentPosition
    val index = chapters.indexOfLast { positionMs >= it.startMs }.coerceAtLeast(0)
    val chapterStartMs = chapters[index].startMs
    val chapterDurationMs = chapters[index].durationMs

    return state.buildUpon()
      .setPlaylist(chapterPlaylist(chapters, state.playlist.first().mediaItem))
      .setCurrentMediaItemIndex(index)
      .setContentPositionMs {
        (wrapped.currentPosition - chapterStartMs).coerceIn(0L, chapterDurationMs)
      }
      .setContentBufferedPositionMs {
        (wrapped.bufferedPosition - chapterStartMs).coerceIn(0L, chapterDurationMs)
      }
      .setAvailableCommands(
        state.availableCommands.buildUpon()
          .addAll(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
          )
          .build(),
      )
      .build()
  }

  override fun handleSeek(
    mediaItemIndex: Int,
    positionMs: Long,
    seekCommand: Int,
  ): ListenableFuture<*> {
    val chapters = host.activeChapters()
      ?: return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    if (chapters.isEmpty()) return super.handleSeek(mediaItemIndex, positionMs, seekCommand)

    when (seekCommand) {
      Player.COMMAND_SEEK_TO_MEDIA_ITEM,
      Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
      Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
      -> {
        val chapter = chapters.getOrNull(resolveIndex(mediaItemIndex, chapters))
          ?: return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
        val within = positionMs.takeIf { it != C.TIME_UNSET } ?: 0L
        host.seekToAbsolute((chapter.startMs + within).milliseconds)
      }

      Player.COMMAND_SEEK_TO_NEXT,
      Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
      -> {
        // Remote controllers can prefer time jumps over chapter skips — the same behavior
        // RemoteControlForwardingPlayer applies on chapter-granular queues
        if (remoteJumpPreferred()) wrapped.seekForward() else host.skipToNextChapter()
      }

      Player.COMMAND_SEEK_TO_PREVIOUS,
      Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
      -> {
        if (remoteJumpPreferred()) wrapped.seekBack() else host.skipToPreviousChapter()
      }

      else -> return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }

    invalidateState()
    return Futures.immediateVoidFuture()
  }

  private fun resolveIndex(mediaItemIndex: Int, chapters: List<Chapter>): Int {
    if (mediaItemIndex != C.INDEX_UNSET) return mediaItemIndex
    val positionMs = wrapped.currentPosition
    return chapters.indexOfLast { positionMs >= it.startMs }.coerceAtLeast(0)
  }

  private fun remoteJumpPreferred(): Boolean {
    return session.isRemoteControllerRequest(appPackageName) && !settings.remoteNextPrevSkipsChapters
  }

  private fun chapterPlaylist(chapters: List<Chapter>, baseItem: MediaItem): List<MediaItemData> {
    cachedPlaylist.takeIf { chapters === cachedChapters }?.let { return it }
    return chapters.map { chapter ->
      MediaItemData.Builder("chapter_${chapter.id}")
        .setMediaItem(
          baseItem.buildUpon()
            .setMediaId("${baseItem.mediaId}_chapter_${chapter.id}")
            .setMediaMetadata(
              baseItem.mediaMetadata.buildUpon()
                .setTitle(chapter.title)
                .setSubtitle(baseItem.mediaMetadata.title)
                .setDurationMs(chapter.durationMs)
                .build(),
            )
            .build(),
        )
        .setDurationUs(chapter.durationMs * 1_000)
        .setIsSeekable(true)
        .build()
    }.also {
      cachedChapters = chapters
      cachedPlaylist = it
    }
  }

  private val Chapter.startMs: Long get() = start.seconds.inWholeMilliseconds
  private val Chapter.durationMs: Long get() = duration.inWholeMilliseconds
}
