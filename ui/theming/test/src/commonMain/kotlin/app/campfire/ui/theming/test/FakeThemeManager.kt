package app.campfire.ui.theming.test

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import app.campfire.ui.theming.api.SwatchSelector
import app.campfire.ui.theming.api.ThemeManager
import com.r0adkll.swatchbuckler.color.dynamiccolor.ColorSpec
import com.r0adkll.swatchbuckler.compose.Schema
import com.r0adkll.swatchbuckler.compose.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeThemeManager : ThemeManager {

  var isInitialized = false
  override fun initialize() {
    isInitialized = true
  }

  val quantizeTasks = mutableListOf<QuantizeTask>()
  override suspend fun enqueue(key: String, image: ImageBitmap) {
    quantizeTasks += QuantizeTask(key, image)
  }

  val themeTasks = mutableListOf<ThemeTask>()
  override suspend fun enqueue(key: String, seedColor: Color) {
    themeTasks += ThemeTask(key, seedColor)
  }

  val themeFlow = MutableSharedFlow<Theme?>(replay = 1)
  override fun observeThemeFor(
    key: String,
    colorSelector: SwatchSelector,
    schema: Schema,
    contrast: Double,
    spec: ColorSpec.SpecVersion,
  ): Flow<Theme?> {
    return themeFlow.asSharedFlow()
  }

  data class QuantizeTask(val key: String, val image: ImageBitmap)
  data class ThemeTask(val key: String, val seedColor: Color)
}
