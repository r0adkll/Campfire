package app.campfire.settings

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesTo

expect interface PreferencesPlatformComponent

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent
