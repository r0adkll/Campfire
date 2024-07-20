// Copyright 2023, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.shared

import androidx.compose.ui.unit.Density
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.app.Flavor
import app.campfire.core.di.AppScope
import app.campfire.core.di.MergeAppScope
import app.campfire.shared.di.SharedAppComponent
import com.r0adkll.kotlininject.merge.annotations.MergeComponent
import java.util.prefs.Preferences
import me.tatarka.inject.annotations.Provides

@AppScope
@MergeComponent(MergeAppScope::class)
abstract class DesktopApplicationComponent : SharedAppComponent {

  @AppScope
  @Provides
  fun provideApplicationId(): app.campfire.core.app.ApplicationInfo = app.campfire.core.app.ApplicationInfo(
    packageName = "app.deckbox",
    debugBuild = true,
    flavor = app.campfire.core.app.Flavor.Standard,
    versionName = "1.0.0",
    versionCode = 1,
  )

  @AppScope
  @Provides
  fun providePreferences(): Preferences = Preferences.userRoot().node("app.deckbox")

  @Provides
  fun provideDensity(): Density = Density(density = 1f) // FIXME

//    @AppScope
//    @Provides
//    fun provideOkHttpClient(
//        // interceptors: Set<Interceptor>,
//    ): OkHttpClient = OkHttpClient.Builder()
//        // .apply { interceptors.forEach(::addInterceptor) }
//        // Adjust the Connection pool to account for historical use of 3 separate clients
//        // but reduce the keepAlive to 2 minutes to avoid keeping radio open.
//        .connectionPool(ConnectionPool(10, 2, TimeUnit.MINUTES))
//        .dispatcher(
//            Dispatcher().apply {
//                // Allow for increased number of concurrent image fetches on same host
//                maxRequestsPerHost = 10
//            },
//        )
//        // Increase timeouts
//        .connectTimeout(20, TimeUnit.SECONDS)
//        .readTimeout(20, TimeUnit.SECONDS)
//        .writeTimeout(20, TimeUnit.SECONDS)
//        .build()

  companion object
}
