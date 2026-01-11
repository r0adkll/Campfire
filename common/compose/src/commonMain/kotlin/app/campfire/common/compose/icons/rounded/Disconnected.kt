package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Disconnected: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Rounded.Disconnected",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(20.98f, 1.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20.293f, 2.293f)
      lineTo(18.828f, 3.758f)
      lineTo(17.949f, 2.879f)
      curveTo(16.817f, 1.746f, 14.84f, 1.745f, 13.707f, 2.879f)
      lineTo(11.5f, 5.086f)
      lineTo(10.707f, 4.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9.99f, 3.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9.293f, 5.707f)
      lineTo(11.086f, 7.5f)
      lineTo(9.293f, 9.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 10.707f, 10.707f)
      lineTo(12.5f, 8.914f)
      lineTo(15.086f, 11.5f)
      lineTo(13.293f, 13.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 14.707f, 14.707f)
      lineTo(16.5f, 12.914f)
      lineTo(18.293f, 14.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 19.707f, 13.293f)
      lineTo(18.914f, 12.5f)
      lineTo(21.121f, 10.293f)
      curveTo(22.291f, 9.123f, 22.291f, 7.221f, 21.121f, 6.051f)
      lineTo(20.242f, 5.172f)
      lineTo(21.707f, 3.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20.98f, 1.99f)
      close()
      moveTo(4.99f, 8.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.293f, 10.707f)
      lineTo(5.086f, 11.5f)
      lineTo(2.879f, 13.707f)
      curveTo(1.709f, 14.877f, 1.709f, 16.779f, 2.879f, 17.949f)
      lineTo(3.758f, 18.828f)
      lineTo(2.293f, 20.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 3.707f, 21.707f)
      lineTo(5.172f, 20.242f)
      lineTo(6.051f, 21.121f)
      curveTo(6.617f, 21.687f, 7.37f, 22f, 8.172f, 22f)
      curveTo(8.973f, 22f, 9.727f, 21.688f, 10.293f, 21.121f)
      lineTo(12.5f, 18.914f)
      lineTo(13.293f, 19.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 14.707f, 18.293f)
      lineTo(5.707f, 9.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.99f, 8.99f)
      close()
    }
  }.build()
}
