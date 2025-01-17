package app.campfire.audioplayer.impl.player

import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyExternalContentIdentifier
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate

object NowPlaying {
  private val infoMutex = Mutex()
  private val info = mutableMapOf<Any?, Any?>()

  suspend fun update(
    currentTime: Duration,
    currentDuration: Duration,
    defaultRate: Double,
    rate: Double,
    metadata: MediaItem.Metadata,
  ) = infoMutex.withLock {
    // Set playback information
    info[MPMediaItemPropertyPlaybackDuration] = currentDuration.toDouble(DurationUnit.SECONDS)
    info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentTime.toDouble(DurationUnit.SECONDS)
    info[MPNowPlayingInfoPropertyPlaybackRate] = rate
    info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = defaultRate

    // Set metadata
    info[MPMediaItemPropertyTitle] = metadata.title
    info[MPMediaItemPropertyArtist] = metadata.artist
    info[MPMediaItemPropertyAlbumTitle] = metadata.albumTitle

    dispatch()
  }

  suspend fun update(
    defaultRate: Double,
    rate: Double,
  ) = infoMutex.withLock {
    info[MPNowPlayingInfoPropertyPlaybackRate] = rate
    info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = defaultRate
    dispatch()
  }

  suspend fun updateSession(
    session: Session,
    artwork: MPMediaItemArtwork? = null,
  ) = infoMutex.withLock {
    if (artwork != null) {
      info[MPMediaItemPropertyArtwork] = artwork
    } else if (shouldFetchCoverImage(session.libraryItem.id)) {
      info[MPMediaItemPropertyArtwork] = null
    }

    info[MPNowPlayingInfoPropertyExternalContentIdentifier] = session.libraryItem.id
    info[MPNowPlayingInfoPropertyIsLiveStream] = false
    info[MPMediaItemPropertyTitle] = session.libraryItem.media.metadata.title
    info[MPMediaItemPropertyArtist] = session.libraryItem.media.metadata.authorName
    info[MPMediaItemPropertyAlbumTitle] = session.libraryItem.media.metadata.title

    dispatch()
  }

  suspend fun reset() = infoMutex.withLock {
    info.clear()
    MPNowPlayingInfoCenter.defaultCenter().setNowPlayingInfo(null)
  }

  private fun shouldFetchCoverImage(itemId: LibraryItemId): Boolean {
    return info[MPNowPlayingInfoPropertyExternalContentIdentifier] != itemId ||
      info[MPMediaItemPropertyArtwork] == null
  }

  private fun dispatch() {
    val filteredInfo = info.filterValues { it != null }
    MPNowPlayingInfoCenter.defaultCenter().setNowPlayingInfo(filteredInfo)
  }
}
