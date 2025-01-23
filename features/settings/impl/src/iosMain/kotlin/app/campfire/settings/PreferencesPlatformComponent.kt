package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import me.tatarka.inject.annotations.Provides
import platform.Foundation.NSUserDefaults

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: NSUserDefaults): ObservableSettings =
    NSUserDefaultsSettings(delegate)
}
