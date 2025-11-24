package app.campfire.ui.theming.initializer

import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.ui.theming.api.ThemeManager
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class ThemeManagerInitializer(
  private val themeManager: Lazy<ThemeManager>,
) : AppInitializer {

  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    themeManager.value.initialize()
  }
}
