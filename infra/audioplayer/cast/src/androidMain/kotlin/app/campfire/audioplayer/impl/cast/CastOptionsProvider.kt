package app.campfire.audioplayer.impl.cast

import android.content.Context
import androidx.media3.cast.DefaultCastOptionsProvider
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class CastOptionsProvider : OptionsProvider {

  @UnstableApi
  override fun getCastOptions(context: Context): CastOptions {
    return CastOptions.Builder()
      .setReceiverApplicationId(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM)
      .setCastMediaOptions(
        CastMediaOptions.Builder()
          .setMediaSessionEnabled(false)
          .setNotificationOptions(null)
          .build(),
      )
      .build()
  }

  override fun getAdditionalSessionProviders(p0: Context): List<SessionProvider> {
    return emptyList()
  }
}
