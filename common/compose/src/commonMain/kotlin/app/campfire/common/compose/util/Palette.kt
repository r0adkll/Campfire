package app.campfire.common.compose.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import coil3.BitmapImage
import dev.sasikanth.material.color.utilities.dynamiccolor.MaterialDynamicColors
import dev.sasikanth.material.color.utilities.hct.Hct
import dev.sasikanth.material.color.utilities.quantize.QuantizerCelebi
import dev.sasikanth.material.color.utilities.scheme.SchemeContent
import dev.sasikanth.material.color.utilities.score.Score
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Palette(
  val primary: Color,
  val primaryContainer: Color,
  val secondary: Color,
  val secondaryContainer: Color,
  val surfaceContainer: Color,
)

suspend fun generateColorPalette(
  imageBitmap: ImageBitmap,
  dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Palette = withContext(dispatcher) {
  val seedColor = Score.score(QuantizerCelebi.quantize(imageBitmap.toPixelMap().buffer, 3))[0]

  val scheme = SchemeContent(
    sourceColorHct = Hct.fromInt(seedColor),
    isDark = true,
    contrastLevel = 0.0,
  )

  val dynamicColors = MaterialDynamicColors()
  Palette(
    primary = Color(dynamicColors.primary().getArgb(scheme)),
    primaryContainer = Color(dynamicColors.primaryContainer().getArgb(scheme)),
    secondary = Color(dynamicColors.secondary().getArgb(scheme)),
    secondaryContainer = Color(dynamicColors.secondaryContainer().getArgb(scheme)),
    surfaceContainer = Color(dynamicColors.surfaceContainer().getArgb(scheme)),
  )
}

suspend fun generateColorPaletteNative(
  bitmap: BitmapImage,
  dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Palette? = withContext(dispatcher) {
  val pixels = pixelMap(bitmap) ?: return@withContext null
  val seedColor = Score.score(QuantizerCelebi.quantize(pixels, 3))[0]

  val scheme = SchemeContent(
    sourceColorHct = Hct.fromInt(seedColor),
    isDark = true,
    contrastLevel = 0.0,
  )

  val dynamicColors = MaterialDynamicColors()
  Palette(
    primary = Color(dynamicColors.primary().getArgb(scheme)),
    primaryContainer = Color(dynamicColors.primaryContainer().getArgb(scheme)),
    secondary = Color(dynamicColors.secondary().getArgb(scheme)),
    secondaryContainer = Color(dynamicColors.secondaryContainer().getArgb(scheme)),
    surfaceContainer = Color(dynamicColors.surfaceContainer().getArgb(scheme)),
  )
}

expect fun pixelMap(bitmap: BitmapImage): IntArray?
