// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai.halogen

import app.campfire.common.compose.theme.ColorPalette
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.ui.theming.ai.HalogenResolveResult
import app.campfire.ui.theming.ai.HalogenThemeManager
import app.campfire.ui.theming.api.HalogenStyle
import com.r0adkll.kimchi.annotations.ContributesBinding
import halogen.HalogenConfig
import halogen.HalogenDefaults
import halogen.HalogenLlmAvailability
import halogen.ThemeExpander
import halogen.compose.toMaterial3
import halogen.engine.Halogen
import halogen.engine.HalogenEngine
import halogen.engine.HalogenResult
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

private val POLL_INTERVAL = 5.seconds
private const val MAX_POLL_ATTEMPTS = 60 // ~5 min total before backing off

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultHalogenThemeManager(
  private val dispatcherProvider: DispatcherProvider,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : HalogenThemeManager {

  private val provider = createNanoProvider()

  private val engine: HalogenEngine? by lazy {
    provider?.let {
      Halogen.Builder()
        .provider(it)
        .defaultTheme(HalogenDefaults.materialYou())
        .config(HalogenConfig.Expressive)
        .scope(scope)
        .build()
    }
  }

  private val isAvailable = MutableStateFlow(false)

  init {
    if (provider != null) {
      // Poll Nano availability so the picker reactively shows the entry once the model
      // finishes downloading. Stops polling when ready, or after MAX_POLL_ATTEMPTS to
      // avoid burning cycles on devices that never get Nano.
      scope.launch {
        var attempts = 0
        while (isActive && attempts < MAX_POLL_ATTEMPTS) {
          val ready = runCatching { provider.availability() }
            .getOrNull() == HalogenLlmAvailability.READY
          isAvailable.value = ready
          if (ready) break
          delay(POLL_INTERVAL)
          attempts++
        }
      }
    }
  }

  override fun observeIsAvailable(): StateFlow<Boolean> = isAvailable.asStateFlow()

  override suspend fun resolve(
    key: String,
    prompt: String,
    style: HalogenStyle,
  ): HalogenResolveResult {
    val engine = engine ?: return HalogenResolveResult.Failed(
      "On-device AI theme generation isn't supported on this device.",
    )

    val config = style.toHalogenConfig()
    // Set the engine config so prompt guidance + style hint reflect the requested style.
    engine.config = config

    // Include style in the cache key so two different styles for the same prompt don't
    // collide on Halogen's MemoryThemeCache.
    val styledKey = "$key-${style.name}"

    bark(tag = "Halogen") {
      "resolve start: key=$styledKey, promptLen=${prompt.length}, style=${style.name}"
    }
    val result = engine.resolve(key = styledKey, hint = prompt)
    bark(tag = "Halogen") { "engine.resolve returned: ${result::class.simpleName}" }

    return when (result) {
      is HalogenResult.Success,
      is HalogenResult.Cached,
      is HalogenResult.FromServer,
      -> {
        val spec = result.themeSpec
          ?: return HalogenResolveResult.Failed("Empty theme spec")
        val palette = withContext(dispatcherProvider.computation) {
          val expanded = ThemeExpander.expand(spec, config)
          ColorPalette(
            lightColorScheme = expanded.lightColorScheme.toMaterial3(),
            darkColorScheme = expanded.darkColorScheme.toMaterial3(),
          )
        }
        HalogenResolveResult.Success(
          palette = palette,
          seedColor = palette.lightColorScheme.primary,
        )
      }

      is HalogenResult.ParseError -> {
        bark(tag = "Halogen", priority = LogPriority.ERROR) {
          "ParseError: ${result.message} | raw=${result.rawResponse?.take(500)}"
        }
        HalogenResolveResult.Failed("Gemini Nano returned malformed JSON: ${result.message}")
      }

      HalogenResult.QuotaExceeded -> HalogenResolveResult.Failed(
        "Gemini Nano quota exceeded.",
      )

      HalogenResult.Unavailable -> HalogenResolveResult.Failed(
        "Gemini Nano isn't available right now. The model may still be downloading.",
      )
    }
  }
}

private fun HalogenStyle.toHalogenConfig(): HalogenConfig = when (this) {
  HalogenStyle.Default -> HalogenConfig.Default
  HalogenStyle.Vibrant -> HalogenConfig.Vibrant
  HalogenStyle.Muted -> HalogenConfig.Muted
  HalogenStyle.Monochrome -> HalogenConfig.Monochrome
  HalogenStyle.Punchy -> HalogenConfig.Punchy
  HalogenStyle.Pastel -> HalogenConfig.Pastel
  HalogenStyle.Editorial -> HalogenConfig.Editorial
  HalogenStyle.Expressive -> HalogenConfig.Expressive
}
