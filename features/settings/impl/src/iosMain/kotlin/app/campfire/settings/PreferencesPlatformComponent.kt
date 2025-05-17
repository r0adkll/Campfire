package app.campfire.settings

import app.campfire.core.di.AppScope
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.Foundation.NSUserDefaults

actual interface PreferencesPlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideSettings(delegate: NSUserDefaults): ObservableSettings =
    NSUserDefaultsSettings(delegate)
}
