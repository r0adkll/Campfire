// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.core.Platform
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.preview.libraryItem
import app.campfire.settings.api.StreamingMethod
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DefaultStreamingRoutePredictorTest {

  // region hlsGatesPass

  @Test
  fun `gates pass for a book on Android`() {
    assertThat(
      hlsGatesPass(episodeId = null, platform = Platform.ANDROID),
    ).isTrue()
  }

  @Test
  fun `gates fail for podcast episodes`() {
    assertThat(
      hlsGatesPass(episodeId = "episode_id", platform = Platform.ANDROID),
    ).isFalse()
  }

  @Test
  fun `gates fail off Android`() {
    assertThat(
      hlsGatesPass(episodeId = null, platform = Platform.IOS),
    ).isFalse()
    assertThat(
      hlsGatesPass(episodeId = null, platform = Platform.DESKTOP),
    ).isFalse()
  }

  // endregion

  // region decideHlsRoute — streaming method policy

  @Test
  fun `direct play only never routes to HLS, even for large single-file items`() {
    assertThat(
      decide(largeSingleFileItem(), method = StreamingMethod.DIRECT_PLAY_ONLY),
    ).isFalse()
  }

  @Test
  fun `prefer HLS always routes to HLS, even for small multi-track items`() {
    assertThat(
      decide(smallMultiTrackItem(), method = StreamingMethod.PREFER_HLS),
    ).isTrue()
  }

  @Test
  fun `prefer HLS still respects the gates`() {
    assertThat(
      decide(largeSingleFileItem(), method = StreamingMethod.PREFER_HLS, episodeId = "episode_id"),
    ).isFalse()
    assertThat(
      decide(largeSingleFileItem(), method = StreamingMethod.PREFER_HLS, platform = Platform.DESKTOP),
    ).isFalse()
  }

  // endregion

  // region decideHlsRoute — AUTO heuristic

  @Test
  fun `auto routes a single-file item longer than the threshold to HLS`() {
    assertThat(
      decide(singleTrackItem(duration = 9.hours), method = StreamingMethod.AUTO),
    ).isTrue()
  }

  @Test
  fun `auto routes a single-file item heavier than the size cutoff to HLS`() {
    assertThat(
      decide(
        singleTrackItem(duration = 1.hours, sizeInBytes = HLS_LARGE_FILE_SIZE_BYTES + 1),
        method = StreamingMethod.AUTO,
      ),
    ).isTrue()
  }

  @Test
  fun `auto keeps a short, light single-file item on direct play`() {
    assertThat(
      decide(singleTrackItem(duration = 30.minutes), method = StreamingMethod.AUTO),
    ).isFalse()
  }

  @Test
  fun `auto keeps multi-track items on direct play regardless of length`() {
    assertThat(
      decide(multiTrackItem(trackCount = 12, duration = 40.hours), method = StreamingMethod.AUTO),
    ).isFalse()
  }

  @Test
  fun `auto reads the track count from numTracks on the minified media shape`() {
    // Minified items carry no tracks[] — only the numTracks count
    assertThat(
      decide(minifiedSingleFileItem(duration = 9.hours), method = StreamingMethod.AUTO),
    ).isTrue()
  }

  @Test
  fun `auto respects the configurable threshold`() {
    val item = singleTrackItem(duration = 3.hours)
    assertThat(decide(item, method = StreamingMethod.AUTO, largeItemThreshold = 8.hours)).isFalse()
    assertThat(decide(item, method = StreamingMethod.AUTO, largeItemThreshold = 2.hours)).isTrue()
  }

  // endregion

  private fun decide(
    item: LibraryItem,
    method: StreamingMethod,
    episodeId: PodcastEpisodeId? = null,
    platform: Platform = Platform.ANDROID,
    largeItemThreshold: Duration = 8.hours,
  ): Boolean = decideHlsRoute(
    libraryItem = item,
    episodeId = episodeId,
    platform = platform,
    method = method,
    largeItemThreshold = largeItemThreshold,
  )

  private fun largeSingleFileItem() = singleTrackItem(duration = 20.hours)

  private fun smallMultiTrackItem() = multiTrackItem(trackCount = 10, duration = 2.hours)

  /** An expanded-shape item: tracks[] populated, one track spanning the whole duration. */
  private fun singleTrackItem(
    duration: Duration,
    sizeInBytes: Long = 100L * 1024 * 1024,
  ): LibraryItem = withTracks(trackCount = 1, duration = duration, sizeInBytes = sizeInBytes)

  private fun multiTrackItem(trackCount: Int, duration: Duration): LibraryItem =
    withTracks(trackCount = trackCount, duration = duration, sizeInBytes = 100L * 1024 * 1024)

  /** A minified-shape item: no tracks[], only the numTracks count. */
  private fun minifiedSingleFileItem(duration: Duration): LibraryItem {
    val item = libraryItem(duration = duration, numTracks = 1)
    val media = item.media as Media.Book
    return item.copy(media = media.copy(tracks = emptyList()))
  }

  private fun withTracks(trackCount: Int, duration: Duration, sizeInBytes: Long): LibraryItem {
    val item = libraryItem(duration = duration, numTracks = trackCount)
    val media = item.media as Media.Book
    val trackDuration = duration / trackCount
    return item.copy(
      media = media.copy(
        sizeInBytes = sizeInBytes,
        tracks = (0 until trackCount).map { index ->
          audioTrack(
            index = index + 1,
            startOffsetSeconds = (trackDuration * index).inWholeSeconds.toFloat(),
            durationSeconds = trackDuration.inWholeSeconds.toFloat(),
          )
        },
      ),
    )
  }

  private fun audioTrack(
    index: Int,
    startOffsetSeconds: Float,
    durationSeconds: Float,
  ) = AudioTrack(
    index = index,
    startOffset = startOffsetSeconds,
    duration = durationSeconds,
    title = "Track $index",
    contentUrl = "/api/items/test/file/$index",
    mimeType = "audio/mp4",
    codec = "aac",
    metadata = FileMetadata(
      filename = "track$index.m4b",
      ext = "m4b",
      path = "/audiobooks/test/track$index.m4b",
      relPath = "track$index.m4b",
      size = 0L,
      mtimeMs = 0L,
      ctimeMs = 0L,
      birthtimeMs = 0L,
    ),
    metaTags = null,
  )
}
