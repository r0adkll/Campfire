// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.player

import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlin.time.DurationUnit
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeAudio
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyExternalContentIdentifier
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate

/**
 * The system Now Playing entry (lock screen, Control Center, CarPlay's now-playing screen).
 *
 * Main-thread only. The book is the album, its author the artist, and the current chapter the
 * title; elapsed time and duration are chapter-relative, matching the in-app scrubber.
 */
object NowPlaying {

  private val info = mutableMapOf<Any?, Any?>()

  fun setSession(session: Session) {
    val itemId = session.libraryItem.id
    if (info[MPNowPlayingInfoPropertyExternalContentIdentifier] != itemId) {
      info.remove(MPMediaItemPropertyArtwork)
    }
    val metadata = session.libraryItem.media.metadata
    info[MPNowPlayingInfoPropertyExternalContentIdentifier] = itemId
    info[MPNowPlayingInfoPropertyMediaType] = MPNowPlayingInfoMediaTypeAudio
    info[MPNowPlayingInfoPropertyIsLiveStream] = false
    info[MPMediaItemPropertyTitle] = session.episode?.title ?: metadata.title
    info[MPMediaItemPropertyArtist] = metadata.authorName
    info[MPMediaItemPropertyAlbumTitle] = metadata.title
    dispatch()
  }

  fun setArtwork(artwork: MPMediaItemArtwork) {
    info[MPMediaItemPropertyArtwork] = artwork
    dispatch()
  }

  fun update(
    title: String?,
    elapsed: Duration,
    duration: Duration,
    rate: Double,
    defaultRate: Double,
  ) {
    title?.let { info[MPMediaItemPropertyTitle] = it }
    info[MPMediaItemPropertyPlaybackDuration] = duration.toDouble(DurationUnit.SECONDS)
    info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = elapsed.toDouble(DurationUnit.SECONDS)
    info[MPNowPlayingInfoPropertyPlaybackRate] = rate
    info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = defaultRate
    dispatch()
  }

  fun reset() {
    info.clear()
    MPNowPlayingInfoCenter.defaultCenter().setNowPlayingInfo(null)
  }

  private fun dispatch() {
    MPNowPlayingInfoCenter.defaultCenter().setNowPlayingInfo(info.filterValues { it != null })
  }
}
