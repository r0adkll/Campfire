package app.campfire.account.settings

import app.campfire.core.di.AppScope
import app.campfire.core.logging.bark
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.util.prefs.Preferences

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
//  @TokenSettings FIXME: https://github.com/ZacSweers/metro/issues/444
  fun provideTokenSettings(delegate: Preferences): TokenSettingsHolder {
    bark { "Desktop Preferences: ${delegate.absolutePath()}" }
    return TokenSettingsHolder(PreferencesSettings(delegate))
  }
}
