package app.campfire.common.settings

import app.campfire.core.di.AppScope
import com.r0adkll.kotlininject.merge.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

expect interface PreferencesPlatformComponent

@ContributesTo(AppScope::class)
interface SettingsComponent : PreferencesPlatformComponent {

  val CampfireSettingsImpl.bind: CampfireSettings
    @Provides get() = this
}
