package app.campfire.account.settings

import app.campfire.core.di.AppScope
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
actual interface PlatformTokenSettingsComponent {

  //  @TokenSettings FIXME: https://github.com/ZacSweers/metro/issues/444
  @OptIn(ExperimentalSettingsImplementation::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideTokenSettings(): TokenSettingsHolder = TokenSettingsHolder(KeychainSettings("app.campfire.app.tokens"))
}
