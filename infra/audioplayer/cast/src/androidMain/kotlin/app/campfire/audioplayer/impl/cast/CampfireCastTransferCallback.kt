// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultCastPlayerTransferCallback
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlayerTransferState
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.impl.asPlatformMediaItems
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark

/**
 * Moves playback state across the local/remote player boundary, swapping the queue shape in the
 * process: local playback uses chapter-granular items (with clipping the receiver can't honor),
 * so the remote queue is rebuilt as one item per audio track and the position is translated
 * through the absolute book timeline. Transferring back rebuilds the normal local queue.
 *
 * Falls back to media3's [DefaultCastPlayerTransferCallback] whenever there is no prepared
 * session or the source queue's durations are unusable for timeline math.
 */
@UnstableApi
internal class CampfireCastTransferCallback(
  private val context: Context,
  private val audioPlayerHolder: AudioPlayerHolder,
) : CastPlayer.TransferCallback {

  private val fallback = DefaultCastPlayerTransferCallback()

  override fun transferState(sourcePlayer: Player, targetPlayer: Player) {
    val session = audioPlayerHolder.currentPlayer.value?.preparedSession
    if (session == null) {
      fallback.transferState(sourcePlayer, targetPlayer)
      return
    }

    try {
      val state = PlayerTransferState.fromPlayer(sourcePlayer)
      val absolutePositionMs = state.absolutePositionMs()
      if (absolutePositionMs == null) {
        fallback.transferState(sourcePlayer, targetPlayer)
        return
      }

      val toRemote = targetPlayer is RemoteCastPlayer
      val items = if (toRemote) {
        MediaItemBuilder.buildTracks(session).map { it.asCastMediaItem() }
      } else {
        MediaItemBuilder.build(session).asPlatformMediaItems(context)
      }
      if (items.isEmpty()) {
        fallback.transferState(sourcePlayer, targetPlayer)
        return
      }

      val (index, positionInItemMs) = items.positionAt(absolutePositionMs)
      bark {
        "Transferring playback ${if (toRemote) "to" else "from"} cast: " +
          "absolute=${absolutePositionMs}ms -> item=$index @ ${positionInItemMs}ms"
      }

      state.buildUpon()
        .setMediaItems(items)
        .setCurrentMediaItemIndex(index)
        .setCurrentPosition(positionInItemMs)
        .build()
        .setToPlayer(targetPlayer)
    } catch (e: Throwable) {
      bark(LogPriority.ERROR, throwable = e) { "Cast queue transfer failed; falling back to direct state transfer" }
      fallback.transferState(sourcePlayer, targetPlayer)
    }
  }

  /**
   * The absolute book position of this state: the summed durations of every item before the
   * current one, plus the in-item position. Null when any preceding item lacks a duration.
   */
  private fun PlayerTransferState.absolutePositionMs(): Long? {
    var offsetMs = 0L
    for (index in 0 until currentMediaItemIndex) {
      val durationMs = mediaItems.getOrNull(index)?.mediaMetadata?.durationMs ?: return null
      offsetMs += durationMs
    }
    return offsetMs + currentPosition
  }

  /** Maps an absolute position onto (item index, position within that item), clamping to the end. */
  private fun List<MediaItem>.positionAt(absolutePositionMs: Long): Pair<Int, Long> {
    var offsetMs = 0L
    forEachIndexed { index, item ->
      val durationMs = item.mediaMetadata.durationMs ?: return index to (absolutePositionMs - offsetMs).coerceAtLeast(
        0L,
      )
      if (absolutePositionMs < offsetMs + durationMs) {
        return index to (absolutePositionMs - offsetMs).coerceAtLeast(0L)
      }
      offsetMs += durationMs
    }
    return lastIndex to (absolutePositionMs - (offsetMs - (last().mediaMetadata.durationMs ?: 0L))).coerceAtLeast(0L)
  }
}
