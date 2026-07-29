package app.campfire.ui.theming.ai

import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.AiThemeBuilder
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Inject

/**
 * Real [AiThemeBuilder] contributed when this module is included in the build. Presence
 * of this binding is what flips [isSupported] on — FOSS builds exclude the module and
 * fall back to `NoOpAiThemeBuilder`.
 */
@ContributesBinding(AppScope::class)
@Inject
class HalogenAiThemeBuilder(
  private val halogenThemeManager: HalogenThemeManager,
) : AiThemeBuilder {

  override val isSupported: Boolean = true

  override fun observeIsAvailable(): StateFlow<Boolean> = halogenThemeManager.observeIsAvailable()
}
