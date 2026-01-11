package app.campfire.account.settings

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class TokenSettings

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtraHeaderSettings

/**
 * Component to be implemented by platform configurations and
 * then used to contribute a DI component to provide the settings
 */
expect interface PlatformTokenSettingsComponent

/**
 * This binds the platform component interface implementation to the
 * DI graph.
 */
@ContributesTo(AppScope::class)
interface TokenSettingsComponent : PlatformTokenSettingsComponent
