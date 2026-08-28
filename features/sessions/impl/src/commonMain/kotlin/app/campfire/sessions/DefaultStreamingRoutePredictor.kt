// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.core.Platform
import app.campfire.core.currentPlatform
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.sessions.api.StreamingRoutePredictor
import app.campfire.settings.api.DevSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.StreamingMethod
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultStreamingRoutePredictor(
  private val playbackSettings: PlaybackSettings,
  private val devSettings: DevSettings,
) : StreamingRoutePredictor {

  override fun canStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Boolean {
    return hlsGatesPass(
      episodeId = episodeId,
      platform = currentPlatform,
    )
  }

  override fun wouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Boolean {
    return decideHlsRoute(
      libraryItem = libraryItem,
      episodeId = episodeId,
      platform = currentPlatform,
      method = playbackSettings.streamingMethod,
      largeItemThreshold = devSettings.hlsLargeItemThreshold,
    )
  }

  override fun observeWouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId?): Flow<Boolean> {
    return combine(
      playbackSettings.observeStreamingMethod(),
      devSettings.observeHlsLargeItemThreshold(),
    ) { method, largeItemThreshold ->
      decideHlsRoute(
        libraryItem = libraryItem,
        episodeId = episodeId,
        platform = currentPlatform,
        method = method,
        largeItemThreshold = largeItemThreshold,
      )
    }
  }
}

/**
 * The hard gates HLS delivery rides on, independent of the chosen streaming method:
 * podcast episodes stay direct play (small single files gain nothing from segmenting),
 * and HLS is Android-first while the route proves out.
 *
 * Pure and platform-parameterized so the decision matrix is unit-testable from any target.
 */
internal fun hlsGatesPass(
  episodeId: PodcastEpisodeId?,
  platform: Platform,
): Boolean {
  if (episodeId != null) return false
  return platform == Platform.ANDROID
}

/** The full HLS-vs-direct decision: [hlsGatesPass] plus the streaming-method policy. */
internal fun decideHlsRoute(
  libraryItem: LibraryItem,
  episodeId: PodcastEpisodeId?,
  platform: Platform,
  method: StreamingMethod,
  largeItemThreshold: Duration,
): Boolean {
  if (!hlsGatesPass(episodeId, platform)) return false

  return when (method) {
    StreamingMethod.DIRECT_PLAY_ONLY -> false
    StreamingMethod.PREFER_HLS -> true
    StreamingMethod.AUTO -> isLargeSingleFile(libraryItem, largeItemThreshold)
  }
}

// Progressive playback degrades most on huge single files (seeks re-fetch from byte
// ranges deep into one blob); either signal — very long or very heavy — routes to HLS
internal const val HLS_LARGE_FILE_SIZE_BYTES = 800L * 1024 * 1024

private fun isLargeSingleFile(libraryItem: LibraryItem, largeItemThreshold: Duration): Boolean {
  val media = libraryItem.media
  // tracks[] is only populated on the expanded media shape; numTracks covers minified
  val trackCount = media.tracks.size.takeIf { it > 0 } ?: media.numTracks
  if (trackCount != 1) return false
  return media.duration > largeItemThreshold ||
    media.sizeInBytes > HLS_LARGE_FILE_SIZE_BYTES
}
