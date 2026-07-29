package app.campfire.ui.theming.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Build-level seam for the on-device AI Theme Builder feature. The real implementation is
 * contributed by the optional `:ui:theming:ai` module; builds that exclude that module
 * (e.g. FOSS distributions) fall back to [NoOpAiThemeBuilder] via the injection site's
 * default argument.
 */
interface AiThemeBuilder {

  /**
   * `true` when the AI Theme Builder feature is compiled into this build at all. Gates
   * navigation to `AiThemeBuilderScreen` — when `false` the screen has no registered
   * presenter and existing AI themes are edited with the regular theme builder instead.
   */
  val isSupported: Boolean

  /**
   * `true` when on-device AI theme generation can serve a request right now — i.e. the
   * Nano model is downloaded and ready. Drives whether the AI Theme Builder entry is
   * shown in the theme picker. Updates reactively as the model finishes downloading.
   */
  fun observeIsAvailable(): StateFlow<Boolean>
}

/**
 * Default used when no [AiThemeBuilder] binding is present in the DI graph: the feature
 * is absent from the build, permanently unavailable.
 */
object NoOpAiThemeBuilder : AiThemeBuilder {
  override val isSupported: Boolean = false
  override fun observeIsAvailable(): StateFlow<Boolean> = MutableStateFlow(false)
}
