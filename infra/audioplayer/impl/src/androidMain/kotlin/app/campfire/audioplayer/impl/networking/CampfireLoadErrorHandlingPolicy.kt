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
   */
  private fun LoadErrorInfo.isRetryableHlsSegmentMiss(): Boolean {
    val exception = exception as? InvalidResponseCodeException ?: return false
    if (exception.responseCode != 404) return false
    if (errorCount > HLS_404_RETRY_COUNT) return false
    return exception.dataSpec.uri.path?.contains("/hls/") == true
  }

  companion object {
    private const val HLS_404_RETRY_BASE_DELAY_MS = 750L
    private const val HLS_404_RETRY_MAX_DELAY_MS = 3_000L
    private const val HLS_404_RETRY_COUNT = 6
  }
}
