package app.campfire.common.compose.icons.rounded

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun rememberAnimatingTravellingIcon(): Painter {
  return rememberVectorPainter(
    name = "Rounded.Travelling",
    defaultWidth = 244.dp,
    defaultHeight = 149.dp,
    viewportWidth = 244f,
    viewportHeight = 149f,
    autoMirror = false,
  ) { _, _ ->
    val infiniteTransition = rememberInfiniteTransition()

    Group(
      name = "road",
    ) {
      Path(fill = SolidColor(Color(0xFF2D2D2D))) {
        moveTo(213f, 101f)
        curveTo(206.37f, 101f, 201f, 106.37f, 201f, 113f)
        curveTo(201f, 119.63f, 206.37f, 125f, 213f, 125f)
        horizontalLineTo(24f)
        curveTo(30.63f, 125f, 36f, 119.63f, 36f, 113f)
        curveTo(36f, 106.37f, 30.63f, 101f, 24f, 101f)
        horizontalLineTo(213f)

        moveTo(12f, 77f)
        lineTo(216f, 77f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 228f, 89f)
        lineTo(228f, 89f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 216f, 101f)
        lineTo(12f, 101f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, 89f)
        lineTo(0f, 89f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 77f)

        moveTo(28f, 125f)
        lineTo(232f, 125f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 244f, 137f)
        lineTo(244f, 137f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 232f, 149f)
        lineTo(28f, 149f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 137f)
        lineTo(16f, 137f)
        arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28f, 125f)

        close()
      }

      val lineOn = 16f
      val lineOff = 24f
      val roadLineOffsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = lineOn + lineOff,
        animationSpec = infiniteRepeatable(
          animation = tween(600, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      )

      Path(
        stroke = SolidColor(Color(0xFFF7E96D)),
        strokeLineWidth = 8f,
        strokeLineCap = StrokeCap.Round,
      ) {
        val startX = 54f
        val endX = startX + 130f
        moveTo(startX, 113f)

        var start = startX
        while (start < endX) {
          if (startX == start) {
            val trimmedStep = (lineOn - roadLineOffsetX)
            if (trimmedStep >= 0) {
              horizontalLineToRelative(trimmedStep)
              moveToRelative(lineOff, 0f)
            } else {
              moveToRelative((lineOn + lineOff) - roadLineOffsetX, 0f)
            }
            start += trimmedStep + lineOff
          } else if ((start + lineOn) > endX) {
            horizontalLineTo(endX)
            start += lineOn + lineOff
          } else {
            horizontalLineToRelative(lineOn)
            moveToRelative(lineOff, 0f)
            start += lineOn + lineOff
          }
        }
      }
    }

    val travelMagnitude = 2f
    val travelDurationMs = 300
    val camperTranslationY by infiniteTransition.animateFloat(
      initialValue = -travelMagnitude,
      targetValue = travelMagnitude,
      animationSpec = infiniteRepeatable(
        animation = tween(travelDurationMs, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(travelDurationMs / 2, StartOffsetType.FastForward),
      ),
    )

    val darkPrimary = MaterialTheme.colorScheme.primary // Color(0xFF008AA9)
    val lightPrimary = MaterialTheme.colorScheme.primaryContainer
      .copy(alpha = 0.95f)
      .compositeOver(MaterialTheme.colorScheme.inverseSurface) // Color(0xFF37D0EE)

    Group(
      name = "camper",
      translationY = camperTranslationY,
    ) {
      Path(fill = SolidColor(MaterialTheme.colorScheme.inversePrimary)) {
        moveTo(129.5f, 86.25f)
        horizontalLineTo(80.75f)
        curveTo(76.61f, 86.25f, 73.25f, 82.89f, 73.25f, 78.75f)
        verticalLineTo(26.25f)
        curveTo(73.25f, 22.11f, 76.61f, 18.75f, 80.75f, 18.75f)
        horizontalLineTo(153.88f)
        curveTo(160.09f, 18.75f, 165.13f, 23.79f, 165.13f, 30f)
        curveTo(165.13f, 33.11f, 162.61f, 35.63f, 159.5f, 35.63f)
        horizontalLineTo(142.63f)
        curveTo(137.45f, 35.63f, 133.25f, 39.82f, 133.25f, 45f)
        verticalLineTo(82.5f)
        curveTo(133.25f, 84.57f, 131.57f, 86.25f, 129.5f, 86.25f)
        close()
      }
      Path(fill = SolidColor(lightPrimary)) {
        moveTo(129.5f, 86.25f)
        horizontalLineTo(80.75f)
        curveTo(76.61f, 86.25f, 73.25f, 82.89f, 73.25f, 78.75f)
        verticalLineTo(71.25f)
        horizontalLineTo(133.25f)
        verticalLineTo(82.5f)
        curveTo(133.25f, 84.57f, 131.57f, 86.25f, 129.5f, 86.25f)
        close()
      }
      Path(fill = SolidColor(MaterialTheme.colorScheme.secondary)) {
        moveTo(137f, 86.25f)
        horizontalLineTo(167f)
        curveTo(169.07f, 86.25f, 170.75f, 84.57f, 170.75f, 82.5f)
        verticalLineTo(62.09f)
        curveTo(170.75f, 60.71f, 170.49f, 59.34f, 170f, 58.05f)
        lineTo(162.76f, 39.23f)
        curveTo(161.93f, 37.06f, 159.84f, 35.63f, 157.51f, 35.63f)
        horizontalLineTo(137f)
        curveTo(134.93f, 35.63f, 133.25f, 37.3f, 133.25f, 39.38f)
        verticalLineTo(82.5f)
        curveTo(133.25f, 84.57f, 134.93f, 86.25f, 137f, 86.25f)
        close()
      }

      Path(fill = SolidColor(darkPrimary)) {
        moveTo(103.25f, 35.63f)
        horizontalLineTo(123.88f)
        curveTo(125.95f, 35.63f, 127.63f, 37.3f, 127.63f, 39.38f)
        verticalLineTo(52.5f)
        curveTo(127.63f, 54.57f, 125.95f, 56.25f, 123.88f, 56.25f)
        horizontalLineTo(103.25f)
        curveTo(101.18f, 56.25f, 99.5f, 54.57f, 99.5f, 52.5f)
        verticalLineTo(39.38f)
        curveTo(99.5f, 37.3f, 101.18f, 35.63f, 103.25f, 35.63f)
        close()
      }
      Path(fill = SolidColor(darkPrimary)) {
        moveTo(86.38f, 35.63f)
        horizontalLineTo(90.13f)
        curveTo(92.2f, 35.63f, 93.88f, 37.3f, 93.88f, 39.38f)
        verticalLineTo(45f)
        curveTo(93.88f, 47.07f, 92.2f, 48.75f, 90.13f, 48.75f)
        horizontalLineTo(86.38f)
        curveTo(84.3f, 48.75f, 82.63f, 47.07f, 82.63f, 45f)
        verticalLineTo(39.38f)
        curveTo(82.63f, 37.3f, 84.3f, 35.63f, 86.38f, 35.63f)
        close()
      }
      Path(fill = SolidColor(darkPrimary)) {
        moveTo(136.06f, 24.38f)
        horizontalLineTo(152.94f)
        curveTo(154.49f, 24.38f, 155.75f, 25.64f, 155.75f, 27.19f)
        curveTo(155.75f, 28.74f, 154.49f, 30f, 152.94f, 30f)
        horizontalLineTo(136.06f)
        curveTo(134.51f, 30f, 133.25f, 28.74f, 133.25f, 27.19f)
        curveTo(133.25f, 25.64f, 134.51f, 24.38f, 136.06f, 24.38f)
        close()
      }
      Path(fill = SolidColor(lightPrimary)) {
        moveTo(144.5f, 60f)
        horizontalLineTo(156.99f)
        curveTo(160.03f, 60f, 162.15f, 56.98f, 161.11f, 54.12f)
        lineTo(156.65f, 43.72f)
        curveTo(156.11f, 42.24f, 154.7f, 41.25f, 153.12f, 41.25f)
        horizontalLineTo(144.5f)
        curveTo(142.43f, 41.25f, 140.75f, 42.93f, 140.75f, 45f)
        verticalLineTo(56.25f)
        curveTo(140.75f, 58.32f, 142.43f, 60f, 144.5f, 60f)
        close()
      }
      Path(
        fill = SolidColor(Color.Black),
        fillAlpha = 0.15f,
        strokeAlpha = 0.15f,
      ) {
        moveTo(170.75f, 62.08f)
        verticalLineTo(82.5f)
        curveTo(170.75f, 84.56f, 169.06f, 86.25f, 167f, 86.25f)
        horizontalLineTo(142.63f)
        curveTo(142.63f, 81.71f, 145.85f, 77.93f, 150.13f, 77.06f)
        lineTo(159.52f, 76.89f)
        curveTo(160.55f, 76.88f, 161.38f, 76.05f, 161.38f, 75.02f)
        verticalLineTo(69.38f)
        curveTo(161.38f, 64.26f, 165.48f, 60.09f, 170.56f, 60.02f)
        curveTo(170.68f, 60.69f, 170.75f, 61.39f, 170.75f, 62.08f)
        close()
      }
      Path(
        fill = SolidColor(Color.White),
        fillAlpha = 0.3f,
        strokeAlpha = 0.3f,
      ) {
        moveTo(112.63f, 18.75f)
        curveTo(112.63f, 23.92f, 108.43f, 28.13f, 103.25f, 28.13f)
        horizontalLineTo(82.63f)
        verticalLineTo(48.75f)
        curveTo(82.63f, 53.93f, 78.43f, 58.13f, 73.25f, 58.13f)
        verticalLineTo(26.25f)
        curveTo(73.25f, 22.11f, 76.61f, 18.75f, 80.75f, 18.75f)
        horizontalLineTo(112.63f)
        close()
      }
      Path(fill = SolidColor(Color.White)) {
        moveTo(79.81f, 37.5f)
        curveTo(78.26f, 37.5f, 77f, 36.24f, 77f, 34.69f)
        verticalLineTo(29.06f)
        curveTo(77f, 25.44f, 79.94f, 22.5f, 83.56f, 22.5f)
        horizontalLineTo(89.19f)
        curveTo(90.74f, 22.5f, 92f, 23.76f, 92f, 25.31f)
        curveTo(92f, 26.87f, 90.74f, 28.13f, 89.19f, 28.13f)
        horizontalLineTo(83.56f)
        curveTo(83.05f, 28.13f, 82.63f, 28.55f, 82.63f, 29.06f)
        verticalLineTo(34.69f)
        curveTo(82.63f, 36.24f, 81.36f, 37.5f, 79.81f, 37.5f)
        close()
      }
    }

    Group("Tires") {
      Path(fill = SolidColor(darkPrimary)) {
        moveTo(95.75f, 97.5f)
        curveTo(101.96f, 97.5f, 107f, 92.46f, 107f, 86.25f)
        curveTo(107f, 80.04f, 101.96f, 75f, 95.75f, 75f)
        curveTo(89.54f, 75f, 84.5f, 80.04f, 84.5f, 86.25f)
        curveTo(84.5f, 92.46f, 89.54f, 97.5f, 95.75f, 97.5f)
        close()
      }
      Path(fill = SolidColor(lightPrimary)) {
        moveTo(95.75f, 91.88f)
        curveTo(98.86f, 91.88f, 101.38f, 89.36f, 101.38f, 86.25f)
        curveTo(101.38f, 83.14f, 98.86f, 80.63f, 95.75f, 80.63f)
        curveTo(92.64f, 80.63f, 90.13f, 83.14f, 90.13f, 86.25f)
        curveTo(90.13f, 89.36f, 92.64f, 91.88f, 95.75f, 91.88f)
        close()
      }
      Path(fill = SolidColor(darkPrimary)) {
        moveTo(152f, 97.5f)
        curveTo(158.21f, 97.5f, 163.25f, 92.46f, 163.25f, 86.25f)
        curveTo(163.25f, 80.04f, 158.21f, 75f, 152f, 75f)
        curveTo(145.79f, 75f, 140.75f, 80.04f, 140.75f, 86.25f)
        curveTo(140.75f, 92.46f, 145.79f, 97.5f, 152f, 97.5f)
        close()
      }
      Path(fill = SolidColor(lightPrimary)) {
        moveTo(152f, 91.88f)
        curveTo(155.11f, 91.88f, 157.63f, 89.36f, 157.63f, 86.25f)
        curveTo(157.63f, 83.14f, 155.11f, 80.63f, 152f, 80.63f)
        curveTo(148.89f, 80.63f, 146.38f, 83.14f, 146.38f, 86.25f)
        curveTo(146.38f, 89.36f, 148.89f, 91.88f, 152f, 91.88f)
        close()
      }
    }
  }
}
