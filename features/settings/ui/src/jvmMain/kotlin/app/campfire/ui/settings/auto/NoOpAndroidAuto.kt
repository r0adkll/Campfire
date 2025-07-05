package app.campfire.ui.settings.auto

import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class NoOpAndroidAuto : AndroidAuto {
  override fun isAvailable(): Boolean = false
  override fun openSettings() = Unit
}
