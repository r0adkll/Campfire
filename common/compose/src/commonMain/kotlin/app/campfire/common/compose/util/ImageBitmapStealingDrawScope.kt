package app.campfire.common.compose.util

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

class ImageBitmapStealingDrawScope(
  private val onImageBitmap: (ImageBitmap) -> Unit,
) : DrawScope {
  override val density: Float
    get() = TODO("Not yet implemented")
  override val drawContext: DrawContext
    get() = TODO("Not yet implemented")
  override val fontScale: Float
    get() = TODO("Not yet implemented")
  override val layoutDirection: LayoutDirection
    get() = TODO("Not yet implemented")

  override fun drawArc(
    brush: Brush,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawCircle(
    brush: Brush,
    radius: Float,
    center: Offset,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawCircle(
    color: Color,
    radius: Float,
    center: Offset,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawImage(
    image: ImageBitmap,
    topLeft: Offset,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    onImageBitmap(image)
  }

  override fun drawImage(
    image: ImageBitmap,
    srcOffset: IntOffset,
    srcSize: IntSize,
    dstOffset: IntOffset,
    dstSize: IntSize,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    onImageBitmap(image)
  }

  override fun drawImage(
    image: ImageBitmap,
    srcOffset: IntOffset,
    srcSize: IntSize,
    dstOffset: IntOffset,
    dstSize: IntSize,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
    filterQuality: FilterQuality
  ) {
    onImageBitmap(image)
  }

  override fun drawLine(
    brush: Brush,
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    cap: StrokeCap,
    pathEffect: PathEffect?,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawLine(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    cap: StrokeCap,
    pathEffect: PathEffect?,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawOval(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawOval(
    color: Color,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawPath(
    path: Path,
    brush: Brush,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawPath(
    path: Path,
    color: Color,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawPoints(
    points: List<Offset>,
    pointMode: PointMode,
    brush: Brush,
    strokeWidth: Float,
    cap: StrokeCap,
    pathEffect: PathEffect?,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawPoints(
    points: List<Offset>,
    pointMode: PointMode,
    color: Color,
    strokeWidth: Float,
    cap: StrokeCap,
    pathEffect: PathEffect?,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawRect(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawRect(
    color: Color,
    topLeft: Offset,
    size: Size,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawRoundRect(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float,
    style: DrawStyle,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }

  override fun drawRoundRect(
    color: Color,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    style: DrawStyle,
    alpha: Float,
    colorFilter: ColorFilter?,
    blendMode: BlendMode,
  ) {
    TODO("Not yet implemented")
  }
}
