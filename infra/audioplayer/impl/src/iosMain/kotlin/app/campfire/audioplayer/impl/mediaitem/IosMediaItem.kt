package app.campfire.audioplayer.impl.mediaitem

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import platform.AVFoundation.AVPlayerItem
import platform.Foundation.NSURL.Companion.URLWithString

/**
 * With the constraints of the IOS audio player not having any sort of "clipping" functionality
 * we should instead model our media item here a bit differently. Instead, keeping a media item
 * node as a representation of the audio file that can be played and have a list of [Track] objects
 * to represent the logical units of listening within that item (i.e. chapters)
 */
data class IosMediaItem(
  val id: String,
  val uri: String,

  /**
   * The time in the entire [app.campfire.core.model.LibraryItem] that this media item
   * starts at. For items that are just one big audio file, this should be 0.
   */
  val startOffset: Duration,

  /**
   * The total duration of this media item. If the item is just one big audio file, this
   * will be the total duration of the entire [app.campfire.core.model.LibraryItem]
   */
  val duration: Duration,

  /**
   * The logically list of tracks, or [app.campfire.core.model.Chapter] / Metadata, in this
   * one audio unit. This is used to derive and update the specific information for where the user
   * is in their playback experience for our internal UI as well as notification information.
   */
  val tracks: List<Track>,
) {
  private val range = startOffset..(startOffset + duration)

  operator fun contains(overallTime: Duration): Boolean = overallTime in range

  /**
   * Convert this item into an [AVPlayerItem] that can be passed to an
   * [AVQueuePlayer] instance.
   */
  fun asAVPlayerItem(): AVPlayerItem {
    // TODO: Only supporting streaming at this time. Add support for local offline playback.
    val url = URLWithString(uri)!!
    return AVPlayerItem(url)
  }

  /**
   * Give the [timeInItem] in the current [app.campfire.core.model.LibraryItem]
   * find and return the current track index and information.
   *
   * @param timeInItem the overall playback time that a user is at in the entire book
   * @return [Pair] of index position and the [Track] that aligns with the time in the media item, or null if not
   */
  fun indexedTrackAtItemPosition(timeInItem: Duration): Pair<Int, Track>? {
    val overallTime = startOffset + timeInItem
    if (overallTime !in range) return null

    tracks.forEachIndexed { index, track ->
      if (overallTime in track) {
        return index to track
      }
    }

    return null
  }

  data class Track(
    val id: Int,
    val startMs: Long,
    val endMs: Long,
    val metadata: MediaItem.Metadata,
  ) {
    val start: Duration get() = startMs.milliseconds
    val end: Duration get() = endMs.milliseconds
    val duration: Duration get() = (endMs - startMs).milliseconds

    fun timeInTrack(overallTime: Duration): Duration = overallTime - startMs.milliseconds

    operator fun contains(time: Duration): Boolean = time.inWholeMilliseconds in startMs until endMs
  }
}
