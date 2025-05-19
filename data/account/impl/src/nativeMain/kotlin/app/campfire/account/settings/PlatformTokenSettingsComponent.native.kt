package app.campfire.account.settings

import app.campfire.core.di.AppScope
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  @OptIn(ExperimentalSettingsImplementation::class)
  @SingleIn(AppScope::class)
  @Provides
//  @TokenSettings FIXME: https://github.com/ZacSweers/metro/issues/444
  fun provideTokenSettings(): TokenSettingsHolder = TokenSettingsHolder(KeychainSettings("app.campfire.app.tokens"))
}
