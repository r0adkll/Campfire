package app.campfire.audioplayer.impl.networking

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo

@UnstableApi
class CampfireLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {
  override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
    if (loadErrorInfo.exception is NonRetryableAuthException) {
      return C.TIME_UNSET
    }
    return super.getRetryDelayMsFor(loadErrorInfo)
  }
}
