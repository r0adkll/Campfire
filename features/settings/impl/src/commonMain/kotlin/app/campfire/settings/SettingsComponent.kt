package app.campfire.settings

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesTo

expect interface PreferencesPlatformComponent

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent
