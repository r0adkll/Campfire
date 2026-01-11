package app.campfire.common.compose.icons.rounded

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons

val CampfireIcons.Rounded.Connected: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
  ImageVector.Builder(
    name = "Rounded.Connected",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    path(fill = SolidColor(Color.Black)) {
      moveTo(20.98f, 1.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20.293f, 2.293f)
      lineTo(16.828f, 5.758f)
      lineTo(15.949f, 4.879f)
      curveTo(14.816f, 3.746f, 12.841f, 3.745f, 11.707f, 4.879f)
      lineTo(10.5f, 6.086f)
      lineTo(9.707f, 5.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8.99f, 4.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8.293f, 6.707f)
      lineTo(17.293f, 15.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 18.707f, 14.293f)
      lineTo(17.914f, 13.5f)
      lineTo(19.121f, 12.293f)
      curveTo(20.291f, 11.123f, 20.291f, 9.221f, 19.121f, 8.051f)
      lineTo(18.242f, 7.172f)
      lineTo(21.707f, 3.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20.98f, 1.99f)
      close()
      moveTo(5.99f, 7.99f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.293f, 9.707f)
      lineTo(6.086f, 10.5f)
      lineTo(4.879f, 11.707f)
      curveTo(3.709f, 12.877f, 3.709f, 14.779f, 4.879f, 15.949f)
      lineTo(5.758f, 16.828f)
      lineTo(2.293f, 20.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 3.707f, 21.707f)
      lineTo(7.172f, 18.242f)
      lineTo(8.051f, 19.121f)
      curveTo(8.617f, 19.687f, 9.37f, 20f, 10.172f, 20f)
      curveTo(10.973f, 20f, 11.727f, 19.688f, 12.293f, 19.121f)
      lineTo(13.5f, 17.914f)
      lineTo(14.293f, 18.707f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 15.707f, 17.293f)
      lineTo(6.707f, 8.293f)
      arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.99f, 7.99f)
      close()
    }
  }.build()
}
