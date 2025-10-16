package app.campfire.analytics.mixpanel.di

import android.app.Application
import app.campfire.analytics.mixpanel.BuildConfig
import app.campfire.analytics.mixpanel.MixPanelFacade
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.mixpanel.android.mpmetrics.MixpanelAPI
import me.tatarka.inject.annotations.Provides

actual interface PlatformMixPanelComponent {

  @Provides
  @SingleIn(AppScope::class)
  fun provideMixPanelFacade(
    application: Application,
    applicationInfo: ApplicationInfo,
  ): MixPanelFacade {
    return MixPanelFacade(
      MixpanelAPI.getInstance(
        /*context=*/
        application,
        /*token=*/
        BuildConfig.MIXPANEL_TOKEN,
        /*optOutTrackingDefault=*/
        true,
        /*trackAutomaticEvents=*/
        true,
      ).apply {
        setUseIpAddressForGeolocation(false)
        setEnableLogging(applicationInfo.debugBuild)
      },
    )
  }
}
