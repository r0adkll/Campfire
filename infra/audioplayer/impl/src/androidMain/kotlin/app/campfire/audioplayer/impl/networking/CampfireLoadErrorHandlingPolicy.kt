// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.networking

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

@UnstableApi
class CampfireLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {
  override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
    if (loadErrorInfo.exception is NonRetryableAuthException) {
      return C.TIME_UNSET
    }
    if (loadErrorInfo.isRetryableHlsSegmentMiss()) {
      return (HLS_404_RETRY_BASE_DELAY_MS * loadErrorInfo.errorCount)
        .coerceAtMost(HLS_404_RETRY_MAX_DELAY_MS)
    }
    return super.getRetryDelayMsFor(loadErrorInfo)
  }

  /**
   * A 404 on an ABS `/hls/` segment usually means ffmpeg hasn't produced it yet — the server
   * reacts to the miss by restarting the transcode at that position, so the segment appears
   * shortly after. The default policy treats 404 as fatal-ish (single quick retry chain);
   * these deserve patient, spaced retries instead.
   *
   * The playlist itself is the opposite case: it exists from the moment the session opens,
   * so a 404 there means the session is dead (server restarted, or replaced by the
   * per-device dedupe). Retry it only briefly — failing fast hands playback to the
   * direct-play fallback instead of stalling for seconds on a stream that can't recover.
   */
  private fun LoadErrorInfo.isRetryableHlsSegmentMiss(): Boolean {
    val exception = exception as? InvalidResponseCodeException ?: return false
    if (exception.responseCode != 404) return false
    val path = exception.dataSpec.uri.path ?: return false
    if (!path.contains("/hls/")) return false
    val maxRetries = if (path.endsWith(".m3u8")) HLS_PLAYLIST_404_RETRY_COUNT else HLS_404_RETRY_COUNT
    return errorCount <= maxRetries
  }

  companion object {
    private const val HLS_404_RETRY_BASE_DELAY_MS = 750L
    private const val HLS_404_RETRY_MAX_DELAY_MS = 3_000L
    private const val HLS_404_RETRY_COUNT = 6
    private const val HLS_PLAYLIST_404_RETRY_COUNT = 1
  }
}
