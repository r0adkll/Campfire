package app.campfire.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import app.campfire.core.di.AppScope
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: AppSharedPreferences): ObservableSettings {
    return SharedPreferencesSettings(delegate)
  }

  @SingleIn(AppScope::class)
  @Provides
  fun provideAppPreferences(
    context: Application,
  ): AppSharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)
}

typealias AppSharedPreferences = SharedPreferences
