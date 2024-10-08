package app.campfire.account.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import me.tatarka.inject.annotations.Provides

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @OptIn(ExperimentalSettingsImplementation::class)
  @SingleIn(AppScope::class)
  @Provides
  @TokenSettings
  fun provideTokenSettings(): Settings = KeychainSettings("app.campfire.app.tokens")
}
