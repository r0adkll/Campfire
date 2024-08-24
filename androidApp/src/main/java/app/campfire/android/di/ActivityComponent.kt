// Copyright 2022, Google LLC, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.android.di

import android.app.Activity
import androidx.core.os.ConfigurationCompat
import app.campfire.core.di.ActivityScope
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.shared.root.CampfireContent
import com.r0adkll.kimchi.annotations.ContributesSubcomponent
import java.util.Locale
import me.tatarka.inject.annotations.Provides

@SingleIn(ActivityScope::class)
@ContributesSubcomponent(
  scope = ActivityScope::class,
  parentScope = AppScope::class,
)
interface ActivityComponent {
  val campfireContent: CampfireContent

  @Provides
  fun provideActivityLocale(activity: Activity): Locale {
    return ConfigurationCompat.getLocales(activity.resources.configuration)
      .get(0) ?: Locale.getDefault()
  }

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(activity: Activity): ActivityComponent
  }
}
