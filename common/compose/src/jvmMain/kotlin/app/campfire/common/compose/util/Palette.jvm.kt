package app.campfire.common.compose.util

import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import coil3.BitmapImage

actual fun pixelMap(bitmap: BitmapImage): IntArray? {
  return bitmap.bitmap.asComposeImageBitmap().toPixelMap().buffer
}
