package app.campfire.common.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import me.tatarka.inject.annotations.Provides
import java.util.prefs.Preferences

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: Preferences): ObservableSettings = PreferencesSettings(delegate)
}
