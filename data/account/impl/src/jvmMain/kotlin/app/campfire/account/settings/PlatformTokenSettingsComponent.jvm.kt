package app.campfire.account.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.bark
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences
import me.tatarka.inject.annotations.Provides

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @SingleIn(AppScope::class)
  @Provides
  @TokenSettings
  fun provideTokenSettings(delegate: Preferences): Settings {
    bark { "Desktop Preferences: ${delegate.absolutePath()}" }
    return PreferencesSettings(delegate)
  }
}
