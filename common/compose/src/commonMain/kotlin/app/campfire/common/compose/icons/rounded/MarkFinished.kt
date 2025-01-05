package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.MarkFinished: ImageVector
  get() {
    if (_MarkFinished != null) {
      return _MarkFinished!!
    }
    _MarkFinished = ImageVector.Builder(
      name = "MarkFinished_Rounded",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 960f,
      viewportHeight = 960f,
    ).apply {
      path(fill = SolidColor(Color(0xFFE8EAED))) {
        moveTo(480f, 900f)
        quadToRelative(-13f, 0f, -25f, -4f)
        reflectiveQuadToRelative(-23f, -12f)
        lineTo(192f, 704f)
        quadToRelative(-15f, -11f, -23.5f, -28f)
        reflectiveQuadToRelative(-8.5f, -36f)
        verticalLineToRelative(-480f)
        quadToRelative(0f, -33f, 23.5f, -56.5f)
        reflectiveQuadTo(240f, 80f)
        horizontalLineToRelative(480f)
        quadToRelative(33f, 0f, 56.5f, 23.5f)
        reflectiveQuadTo(800f, 160f)
        verticalLineToRelative(480f)
        quadToRelative(0f, 19f, -8.5f, 36f)
        reflectiveQuadTo(768f, 704f)
        lineTo(528f, 884f)
        quadToRelative(-11f, 8f, -23f, 12f)
        reflectiveQuadToRelative(-25f, 4f)
        close()
        moveTo(480f, 820f)
        lineTo(720f, 640f)
        verticalLineToRelative(-480f)
        lineTo(240f, 160f)
        verticalLineToRelative(480f)
        lineToRelative(240f, 180f)
        close()
        moveTo(438f, 486f)
        lineTo(382f, 430f)
        quadToRelative(-12f, -12f, -28f, -11.5f)
        reflectiveQuadTo(326f, 430f)
        quadToRelative(-12f, 12f, -12.5f, 28.5f)
        reflectiveQuadTo(325f, 487f)
        lineToRelative(85f, 85f)
        quadToRelative(12f, 12f, 28f, 12f)
        reflectiveQuadToRelative(28f, -12f)
        lineToRelative(170f, -170f)
        quadToRelative(12f, -12f, 11.5f, -28f)
        reflectiveQuadTo(636f, 346f)
        quadToRelative(-12f, -12f, -28.5f, -12.5f)
        reflectiveQuadTo(579f, 345f)
        lineTo(438f, 486f)
        close()
        moveTo(480f, 160f)
        lineTo(240f, 160f)
        horizontalLineToRelative(480f)
        horizontalLineToRelative(-240f)
        close()
      }
    }.build()

    return _MarkFinished!!
  }

@Suppress("ObjectPropertyName")
private var _MarkFinished: ImageVector? = null
