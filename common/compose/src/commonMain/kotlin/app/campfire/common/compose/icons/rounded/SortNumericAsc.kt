package app.campfire.common.compose.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _SortNumericAsc: ImageVector? = null

val Icons.Rounded.SortNumericAsc: ImageVector
  get() {
    if (_SortNumericAsc != null) {
      return _SortNumericAsc!!
    }
    _SortNumericAsc = ImageVector.Builder(
      name = "SortNumericAsc",
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
    ).apply {
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFF000000)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(5.25f, 4f)
        curveTo(5.5f, 3.5f, 6f, 3f, 7f, 3f)
        curveToRelative(2f, 0f, 2f, 2f, 2f, 2f)
        curveToRelative(0f, 1.706f, -4f, 3.891f, -4f, 3.891f)
        verticalLineTo(9f)
        horizontalLineToRelative(4f)
      }
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(8f, 21f)
        lineTo(8f, 21f)
        curveToRelative(-0.552f, 0f, -1f, -0.448f, -1f, -1f)
        verticalLineToRelative(-4.61f)
        lineToRelative(-0.979f, 0.484f)
        curveTo(5.423f, 16.013f, 4.983f, 15.703f, 5f, 15.14f)
        curveToRelative(0f, -0.413f, 0.254f, -0.783f, 0.638f, -0.932f)
        lineToRelative(2.229f, -1.157f)
        curveTo(8.419f, 12.852f, 9f, 13.261f, 9f, 13.847f)
        verticalLineTo(20f)
        curveTo(9f, 20.552f, 8.552f, 21f, 8f, 21f)
        close()
      }
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1.0f,
        stroke = null,
        strokeAlpha = 1.0f,
        strokeLineWidth = 1.0f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 1.0f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(17.566f, 21.765f)
        lineToRelative(2.4f, -2.4f)
        curveToRelative(0.229f, -0.229f, 0.297f, -0.572f, 0.174f, -0.872f)
        curveTo(20.015f, 18.194f, 19.724f, 18f, 19.4f, 18f)
        horizontalLineToRelative(-4.8f)
        curveToRelative(-0.323f, 0f, -0.615f, 0.194f, -0.739f, 0.494f)
        curveTo(13.82f, 18.593f, 13.8f, 18.697f, 13.8f, 18.8f)
        curveToRelative(0f, 0.208f, 0.082f, 0.413f, 0.234f, 0.566f)
        lineToRelative(2.4f, 2.4f)
        curveTo(16.747f, 22.078f, 17.254f, 22.078f, 17.566f, 21.765f)
        close()
      }
      path(
        fill = null,
        fillAlpha = 1.0f,
        stroke = SolidColor(Color(0xFF000000)),
        strokeAlpha = 1.0f,
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Miter,
        strokeLineMiter = 10f,
        pathFillType = PathFillType.NonZero,
      ) {
        moveTo(17f, 20f)
        lineTo(17f, 3f)
      }
    }.build()
    return _SortNumericAsc!!
  }
