package app.campfire.common.compose.icons.rounded

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.InfiniteTransition
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.DefaultPathName
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.graphics.vector.DefaultStrokeLineJoin
import androidx.compose.ui.graphics.vector.DefaultStrokeLineMiter
import androidx.compose.ui.graphics.vector.DefaultStrokeLineWidth
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.VectorComposable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun rememberAnimatedEmptyState(): Painter {
  return rememberVectorPainter(
    name = "Rounded.EmptyState",
    defaultWidth = 278.dp,
    defaultHeight = 333.dp,
    viewportWidth = 278f,
    viewportHeight = 333f,
    autoMirror = false,
  ) { _, _ ->
    val infiniteTransition = rememberInfiniteTransition()

    Mountain()
    Grass()
    Trees(infiniteTransition)
    Sun(infiniteTransition)
    Road(infiniteTransition)
    Camper(infiniteTransition)
  }
}

@VectorComposable
@Composable
private fun Mountain() {
  Group(
    name = "Mountain",
  ) {
    Path(fill = SolidColor(Color(0xFFA7B3C7))) {
      moveTo(39.51f, 37.29f)
      lineTo(6.42f, 83.11f)
      curveTo(2.25f, 88.89f, 0f, 95.84f, 0f, 102.97f)
      verticalLineTo(147.15f)
      horizontalLineTo(125f)
      lineTo(55.39f, 37.76f)
      curveTo(51.75f, 32.04f, 43.48f, 31.79f, 39.51f, 37.29f)
      close()
    }
    Path(fill = SolidColor(Color(0xFF68E5FD))) {
      moveTo(28.71f, 63.89f)
      curveTo(30.83f, 66.85f, 35.85f, 67.06f, 38.31f, 64.29f)
      lineTo(41.99f, 61.3f)
      curveTo(44.8f, 58.14f, 50.44f, 58.13f, 53.26f, 61.28f)
      lineTo(57.02f, 64.33f)
      curveTo(59.49f, 67.08f, 64.49f, 66.86f, 66.6f, 63.91f)
      lineTo(69.47f, 59.89f)
      lineTo(55.39f, 37.76f)
      curveTo(51.75f, 32.04f, 43.48f, 31.79f, 39.51f, 37.29f)
      lineTo(24.53f, 58.03f)
      lineTo(28.71f, 63.89f)
      close()
    }
    Path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(18.38f, 102.97f)
      curveTo(18.38f, 99.68f, 19.4f, 96.54f, 21.32f, 93.87f)
      lineTo(40.7f, 67.04f)
      curveTo(46.62f, 58.8f, 44.75f, 47.34f, 36.54f, 41.41f)
      lineTo(6.42f, 83.11f)
      curveTo(2.25f, 88.89f, 0f, 95.84f, 0f, 102.97f)
      verticalLineTo(121.35f)
      curveTo(10.15f, 121.35f, 18.38f, 113.13f, 18.38f, 102.97f)
      close()
    }
    Path(fill = SolidColor(Color.White)) {
      moveTo(16.52f, 96.25f)
      curveTo(15.4f, 96.25f, 14.28f, 95.91f, 13.3f, 95.21f)
      curveTo(10.83f, 93.43f, 10.27f, 89.98f, 12.06f, 87.51f)
      lineTo(21.9f, 73.88f)
      curveTo(23.68f, 71.4f, 27.13f, 70.85f, 29.6f, 72.64f)
      curveTo(32.07f, 74.42f, 32.62f, 77.86f, 30.84f, 80.33f)
      lineTo(21f, 93.96f)
      curveTo(19.92f, 95.46f, 18.23f, 96.25f, 16.52f, 96.25f)
      close()
    }
  }
}

@VectorComposable
@Composable
private fun Trees(
  infiniteTransition: InfiniteTransition,
) {
  val scaleX by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(2_000, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse,
    ),
  )
  val scaleY by infiniteTransition.animateFloat(
    initialValue = 1.02f,
    targetValue = 0.98f,
    animationSpec = infiniteRepeatable(
      animation = tween(2_000, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse,
    ),
  )

  Group(
    name = "Tree1",
    scaleX = scaleX,
    scaleY = scaleY,
    pivotX = 204f,
    pivotY = 171f,
  ) {
    Path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(206.51f, 171.03f)
      horizontalLineTo(201.49f)
      curveTo(200.1f, 171.03f, 198.97f, 169.91f, 198.97f, 168.52f)
      verticalLineTo(157.21f)
      horizontalLineTo(209.03f)
      verticalLineTo(168.52f)
      curveTo(209.03f, 169.91f, 207.9f, 171.03f, 206.51f, 171.03f)
      close()
    }
    Path(fill = SolidColor(Color(0xFF5E8700))) {
      moveTo(223.21f, 156.17f)
      lineTo(214.45f, 144.78f)
      curveTo(214.02f, 144.21f, 214.42f, 143.39f, 215.14f, 143.39f)
      horizontalLineTo(216.49f)
      curveTo(218.57f, 143.39f, 219.75f, 141f, 218.48f, 139.34f)
      lineTo(212.04f, 130.96f)
      curveTo(211.6f, 130.39f, 212f, 129.57f, 212.72f, 129.57f)
      curveTo(214.81f, 129.57f, 215.99f, 127.17f, 214.71f, 125.52f)
      lineTo(206.99f, 115.47f)
      curveTo(205.48f, 113.51f, 202.52f, 113.51f, 201.01f, 115.47f)
      lineTo(193.29f, 125.52f)
      curveTo(192.02f, 127.17f, 193.19f, 129.57f, 195.28f, 129.57f)
      curveTo(196f, 129.57f, 196.4f, 130.39f, 195.96f, 130.96f)
      lineTo(189.52f, 139.34f)
      curveTo(188.25f, 141f, 189.42f, 143.39f, 191.51f, 143.39f)
      horizontalLineTo(192.86f)
      curveTo(193.58f, 143.39f, 193.98f, 144.21f, 193.54f, 144.78f)
      lineTo(184.79f, 156.17f)
      curveTo(182.88f, 158.65f, 184.65f, 162.24f, 187.78f, 162.24f)
      horizontalLineTo(220.22f)
      curveTo(223.35f, 162.24f, 225.12f, 158.65f, 223.21f, 156.17f)
      close()
    }
    Path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(193.31f, 145.08f)
      curveTo(194.24f, 144.67f, 195.09f, 144.04f, 195.76f, 143.17f)
      lineTo(202.21f, 134.78f)
      curveTo(203.88f, 132.61f, 204.16f, 129.73f, 202.95f, 127.27f)
      curveTo(202.72f, 126.8f, 202.44f, 126.36f, 202.13f, 125.97f)
      lineTo(207.25f, 119.3f)
      curveTo(207.75f, 118.65f, 208.09f, 117.93f, 208.3f, 117.18f)
      lineTo(206.99f, 115.47f)
      curveTo(205.48f, 113.51f, 202.52f, 113.51f, 201.01f, 115.47f)
      lineTo(193.29f, 125.52f)
      curveTo(192.02f, 127.17f, 193.19f, 129.57f, 195.28f, 129.57f)
      curveTo(196f, 129.57f, 196.4f, 130.39f, 195.96f, 130.96f)
      lineTo(189.52f, 139.34f)
      curveTo(188.25f, 140.99f, 189.42f, 143.39f, 191.51f, 143.39f)
      horizontalLineTo(192.86f)
      curveTo(193.58f, 143.39f, 193.98f, 144.21f, 193.54f, 144.78f)
      lineTo(193.31f, 145.08f)
      close()
    }
    Path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(214.45f, 144.78f)
      curveTo(214.02f, 144.21f, 214.42f, 143.39f, 215.14f, 143.39f)
      horizontalLineTo(216.49f)
      curveTo(218.57f, 143.39f, 219.75f, 140.99f, 218.48f, 139.34f)
      lineTo(212.04f, 130.96f)
      curveTo(211.89f, 130.76f, 211.84f, 130.54f, 211.87f, 130.33f)
      curveTo(211.57f, 130.49f, 211.27f, 130.65f, 211f, 130.87f)
      curveTo(209.31f, 132.17f, 208.45f, 134.86f, 208.54f, 137.22f)
      curveTo(208.59f, 138.59f, 208.13f, 139.88f, 207.48f, 141.09f)
      curveTo(207.47f, 141.09f, 207.47f, 141.09f, 207.47f, 141.1f)
      curveTo(206.26f, 143.55f, 206.54f, 146.43f, 208.22f, 148.61f)
      lineTo(213.86f, 155.96f)
      horizontalLineTo(203.01f)
      curveTo(199.73f, 155.96f, 196.8f, 158.35f, 196.49f, 161.62f)
      curveTo(196.47f, 161.83f, 196.46f, 162.03f, 196.46f, 162.24f)
      horizontalLineTo(220.22f)
      curveTo(223.35f, 162.24f, 225.12f, 158.65f, 223.21f, 156.17f)
      lineTo(214.45f, 144.78f)
      close()
    }
    Path(fill = SolidColor(Color.White)) {
      moveTo(200.86f, 127.05f)
      curveTo(200.5f, 127.05f, 200.13f, 126.95f, 199.81f, 126.74f)
      curveTo(198.95f, 126.16f, 198.71f, 124.99f, 199.29f, 124.12f)
      lineTo(201.8f, 120.35f)
      curveTo(202.38f, 119.49f, 203.55f, 119.25f, 204.42f, 119.83f)
      curveTo(205.28f, 120.41f, 205.52f, 121.58f, 204.94f, 122.44f)
      lineTo(202.43f, 126.21f)
      curveTo(202.06f, 126.76f, 201.46f, 127.05f, 200.86f, 127.05f)
      close()
    }
  }

  val scale2Time = 2_500
  val scale2Offset = 500
  val scaleX2 by infiniteTransition.animateFloat(
    initialValue = 0.93f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(scale2Time, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse,
      initialStartOffset = StartOffset(scale2Offset),
    ),
  )
  val scaleY2 by infiniteTransition.animateFloat(
    initialValue = 1.05f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(scale2Time, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse,
      initialStartOffset = StartOffset(scale2Offset),
    ),
  )
  Group(
    name = "Tree2",
    scaleX = scaleX2,
    scaleY = scaleY2,
    pivotX = 39.5f,
    pivotY = 209.7f,
  ) {
    Path(fill = SolidColor(Color(0xFFDA7200))) {
      moveTo(42.88f, 209.7f)
      horizontalLineTo(36.21f)
      curveTo(34.37f, 209.7f, 32.87f, 208.21f, 32.87f, 206.36f)
      verticalLineTo(191.35f)
      horizontalLineTo(46.22f)
      verticalLineTo(206.36f)
      curveTo(46.22f, 208.21f, 44.72f, 209.7f, 42.88f, 209.7f)
      close()
    }
    Path(fill = SolidColor(Color(0xFF5E8700))) {
      moveTo(65.04f, 189.97f)
      lineTo(53.42f, 174.85f)
      curveTo(52.84f, 174.1f, 53.38f, 173.01f, 54.33f, 173.01f)
      horizontalLineTo(56.12f)
      curveTo(58.89f, 173.01f, 60.45f, 169.83f, 58.77f, 167.64f)
      lineTo(50.21f, 156.51f)
      curveTo(49.63f, 155.75f, 50.17f, 154.66f, 51.12f, 154.66f)
      curveTo(53.89f, 154.66f, 55.45f, 151.48f, 53.77f, 149.29f)
      lineTo(43.51f, 135.96f)
      curveTo(41.51f, 133.35f, 37.58f, 133.35f, 35.58f, 135.96f)
      lineTo(25.33f, 149.29f)
      curveTo(23.64f, 151.48f, 25.2f, 154.66f, 27.97f, 154.66f)
      curveTo(28.92f, 154.66f, 29.46f, 155.75f, 28.88f, 156.51f)
      lineTo(20.32f, 167.64f)
      curveTo(18.64f, 169.83f, 20.2f, 173.01f, 22.97f, 173.01f)
      horizontalLineTo(24.76f)
      curveTo(25.71f, 173.01f, 26.25f, 174.1f, 25.67f, 174.85f)
      lineTo(14.05f, 189.97f)
      curveTo(11.52f, 193.26f, 13.86f, 198.02f, 18.01f, 198.02f)
      horizontalLineTo(61.08f)
      curveTo(65.23f, 198.02f, 67.57f, 193.26f, 65.04f, 189.97f)
      close()
    }
    Path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(25.36f, 175.25f)
      curveTo(26.6f, 174.71f, 27.72f, 173.87f, 28.6f, 172.72f)
      lineTo(37.17f, 161.58f)
      curveTo(39.38f, 158.7f, 39.76f, 154.87f, 38.15f, 151.61f)
      curveTo(37.84f, 150.99f, 37.48f, 150.41f, 37.06f, 149.88f)
      lineTo(43.86f, 141.04f)
      curveTo(44.53f, 140.17f, 44.98f, 139.22f, 45.26f, 138.23f)
      lineTo(43.51f, 135.96f)
      curveTo(41.51f, 133.35f, 37.58f, 133.35f, 35.58f, 135.96f)
      lineTo(25.33f, 149.29f)
      curveTo(23.64f, 151.48f, 25.2f, 154.66f, 27.97f, 154.66f)
      curveTo(28.92f, 154.66f, 29.46f, 155.75f, 28.88f, 156.51f)
      lineTo(20.32f, 167.64f)
      curveTo(18.64f, 169.83f, 20.2f, 173.01f, 22.97f, 173.01f)
      horizontalLineTo(24.76f)
      curveTo(25.71f, 173.01f, 26.25f, 174.1f, 25.67f, 174.85f)
      lineTo(25.36f, 175.25f)
      close()
    }
    Path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(53.42f, 174.85f)
      curveTo(52.84f, 174.1f, 53.38f, 173.01f, 54.33f, 173.01f)
      horizontalLineTo(56.12f)
      curveTo(58.89f, 173.01f, 60.45f, 169.83f, 58.77f, 167.64f)
      lineTo(50.21f, 156.51f)
      curveTo(50.01f, 156.25f, 49.95f, 155.95f, 49.99f, 155.67f)
      curveTo(49.59f, 155.88f, 49.2f, 156.1f, 48.84f, 156.38f)
      curveTo(46.59f, 158.11f, 45.46f, 161.69f, 45.57f, 164.82f)
      curveTo(45.63f, 166.64f, 45.02f, 168.34f, 44.16f, 169.95f)
      curveTo(44.15f, 169.95f, 44.15f, 169.96f, 44.15f, 169.96f)
      curveTo(42.54f, 173.23f, 42.92f, 177.05f, 45.14f, 179.93f)
      lineTo(52.64f, 189.68f)
      horizontalLineTo(38.24f)
      curveTo(33.88f, 189.68f, 29.99f, 192.87f, 29.58f, 197.21f)
      curveTo(29.55f, 197.48f, 29.54f, 197.75f, 29.54f, 198.02f)
      horizontalLineTo(61.08f)
      curveTo(65.23f, 198.02f, 67.57f, 193.26f, 65.04f, 189.97f)
      lineTo(53.42f, 174.85f)
      close()
    }
    Path(fill = SolidColor(Color.White)) {
      moveTo(35.37f, 151.32f)
      curveTo(34.9f, 151.32f, 34.42f, 151.19f, 33.99f, 150.9f)
      curveTo(32.84f, 150.14f, 32.53f, 148.58f, 33.29f, 147.43f)
      lineTo(36.63f, 142.43f)
      curveTo(37.4f, 141.28f, 38.95f, 140.97f, 40.1f, 141.74f)
      curveTo(41.25f, 142.5f, 41.56f, 144.06f, 40.79f, 145.21f)
      lineTo(37.46f, 150.21f)
      curveTo(36.98f, 150.93f, 36.18f, 151.32f, 35.37f, 151.32f)
      close()
    }
  }
}

@VectorComposable
@Composable
private fun Grass() {
  Group(
    name = "Grass",
  ) {
    Path(fill = SolidColor(Color(0xFF98C900))) {
      moveTo(0f, 147f)
      verticalLineTo(282.9f)
      curveTo(0f, 309.26f, 21.24f, 330.68f, 47.59f, 330.9f)
      lineTo(229.59f, 332.44f)
      curveTo(256.25f, 332.67f, 277.99f, 311.11f, 277.99f, 284.44f)
      verticalLineTo(158f)
      curveTo(277.99f, 151.93f, 273.07f, 147f, 266.99f, 147f)
      horizontalLineTo(0f)
      close()
    }
    Path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(238f, 151.62f)
      curveTo(239.89f, 156.88f, 244.87f, 160.67f, 250.76f, 160.67f)
      horizontalLineTo(264.38f)
      verticalLineTo(174.33f)
      curveTo(264.38f, 181.88f, 270.48f, 188f, 278f, 188f)
      verticalLineTo(157.93f)
      curveTo(278f, 151.9f, 273.12f, 147f, 267.11f, 147f)
      horizontalLineTo(245.32f)
      curveTo(242.09f, 147f, 239.33f, 148.89f, 238f, 151.62f)
      close()
    }
  }
}

@VectorComposable
@Composable
private fun Sun(
  infiniteTransition: InfiniteTransition,
) {
  val sunRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(15_000, easing = LinearEasing),
    ),
  )

  Group(
    name = "SunRays",
    rotation = sunRotation,
    pivotX = 196f,
    pivotY = 45f,
  ) {
    Path(fill = SolidColor(Color(0xFFFFCE29))) {
      moveTo(224.17f, 56.97f)
      curveTo(222.36f, 61.33f, 227.99f, 70.77f, 224.73f, 74.04f)
      curveTo(221.46f, 77.3f, 212.01f, 71.67f, 207.66f, 73.48f)
      curveTo(203.46f, 75.21f, 200.82f, 85.94f, 196f, 85.94f)
      curveTo(193.53f, 85.94f, 191.65f, 83.14f, 189.82f, 80.17f)
      curveTo(188.08f, 77.32f, 186.39f, 74.32f, 184.34f, 73.48f)
      curveTo(182.24f, 72.6f, 178.96f, 73.46f, 175.74f, 74.22f)
      curveTo(172.3f, 75.04f, 168.97f, 75.73f, 167.27f, 74.04f)
      curveTo(164.01f, 70.77f, 169.65f, 61.33f, 167.83f, 56.98f)
      curveTo(166.11f, 52.78f, 155.38f, 50.14f, 155.38f, 45.31f)
      curveTo(155.38f, 40.49f, 166.11f, 37.85f, 167.83f, 33.65f)
      curveTo(169.64f, 29.3f, 164.01f, 19.85f, 167.27f, 16.59f)
      curveTo(170.54f, 13.32f, 179.99f, 18.96f, 184.34f, 17.15f)
      curveTo(188.54f, 15.42f, 191.18f, 4.69f, 196f, 4.69f)
      curveTo(200.82f, 4.69f, 203.46f, 15.42f, 207.66f, 17.15f)
      curveTo(209.73f, 18.01f, 212.93f, 17.2f, 216.09f, 16.45f)
      curveTo(219.58f, 15.6f, 223f, 14.86f, 224.73f, 16.59f)
      curveTo(226.45f, 18.31f, 225.71f, 21.73f, 224.86f, 25.22f)
      curveTo(224.12f, 28.39f, 223.3f, 31.58f, 224.17f, 33.65f)
      curveTo(225.89f, 37.85f, 236.63f, 40.49f, 236.63f, 45.31f)
      curveTo(236.63f, 50.14f, 225.89f, 52.78f, 224.17f, 56.97f)
      close()
    }
  }
  Group(
    name = "SunCore",
  ) {
    Path(fill = SolidColor(Color(0xFFFFA500))) {
      moveTo(214.75f, 45.31f)
      curveTo(214.75f, 55.67f, 206.36f, 64.06f, 196f, 64.06f)
      curveTo(193.28f, 64.06f, 190.69f, 63.48f, 188.36f, 62.44f)
      curveTo(184.14f, 60.56f, 180.75f, 57.17f, 178.88f, 52.95f)
      curveTo(177.83f, 50.63f, 177.25f, 48.03f, 177.25f, 45.31f)
      curveTo(177.25f, 34.95f, 185.64f, 26.56f, 196f, 26.56f)
      curveTo(198.72f, 26.56f, 201.31f, 27.14f, 203.64f, 28.19f)
      curveTo(207.86f, 30.06f, 211.25f, 33.45f, 213.13f, 37.67f)
      curveTo(214.17f, 40f, 214.75f, 42.59f, 214.75f, 45.31f)
      close()
    }
    Path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(214.75f, 45.31f)
      curveTo(214.75f, 55.67f, 206.36f, 64.06f, 196f, 64.06f)
      curveTo(193.28f, 64.06f, 190.69f, 63.48f, 188.36f, 62.44f)
      curveTo(189.11f, 58.91f, 192.25f, 56.25f, 196f, 56.25f)
      curveTo(197.63f, 56.25f, 199.17f, 55.91f, 200.61f, 55.22f)
      curveTo(202.97f, 54.13f, 204.81f, 52.28f, 205.89f, 49.97f)
      curveTo(206.59f, 48.48f, 206.94f, 46.94f, 206.94f, 45.31f)
      curveTo(206.94f, 41.56f, 209.59f, 38.42f, 213.13f, 37.67f)
      curveTo(214.17f, 40f, 214.75f, 42.59f, 214.75f, 45.31f)
      close()
    }
    Path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(203.64f, 28.19f)
      curveTo(202.89f, 31.72f, 199.75f, 34.38f, 196f, 34.38f)
      curveTo(194.38f, 34.38f, 192.83f, 34.72f, 191.39f, 35.41f)
      curveTo(189.03f, 36.5f, 187.19f, 38.34f, 186.11f, 40.66f)
      curveTo(185.41f, 42.14f, 185.06f, 43.69f, 185.06f, 45.31f)
      curveTo(185.06f, 49.06f, 182.41f, 52.2f, 178.88f, 52.95f)
      curveTo(177.83f, 50.63f, 177.25f, 48.03f, 177.25f, 45.31f)
      curveTo(177.25f, 34.95f, 185.64f, 26.56f, 196f, 26.56f)
      curveTo(198.72f, 26.56f, 201.31f, 27.14f, 203.64f, 28.19f)
      close()
    }
    Path(fill = SolidColor(Color.White)) {
      moveTo(188.31f, 39.88f)
      curveTo(187.7f, 39.88f, 187.1f, 39.65f, 186.64f, 39.18f)
      curveTo(185.73f, 38.26f, 185.74f, 36.77f, 186.67f, 35.87f)
      curveTo(187.75f, 34.8f, 189.01f, 33.93f, 190.41f, 33.28f)
      curveTo(192.13f, 32.45f, 194.02f, 32.03f, 196f, 32.03f)
      curveTo(197.29f, 32.03f, 198.34f, 33.08f, 198.34f, 34.38f)
      curveTo(198.34f, 35.67f, 197.29f, 36.72f, 196f, 36.72f)
      curveTo(194.73f, 36.72f, 193.51f, 36.99f, 192.4f, 37.52f)
      curveTo(191.47f, 37.95f, 190.65f, 38.52f, 189.96f, 39.2f)
      curveTo(189.5f, 39.65f, 188.9f, 39.88f, 188.31f, 39.88f)
      close()
    }
  }
}

@VectorComposable
@Composable
private fun Road(
  infiniteTransition: InfiniteTransition,
) {
  Group(
    name = "Road",
  ) {
    Path(fill = SolidColor(Color(0xFF2D2D2D))) {
      moveTo(40f, 284f)
      lineTo(244f, 284f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 256f, 296f)
      lineTo(256f, 296f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 244f, 308f)
      lineTo(40f, 308f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28f, 296f)
      lineTo(28f, 296f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 40f, 284f)
      moveTo(231f, 260f)
      curveTo(224.37f, 260f, 219f, 265.37f, 219f, 272f)
      curveTo(219f, 278.63f, 224.37f, 284f, 231f, 284f)
      horizontalLineTo(36f)
      curveTo(42.63f, 284f, 48f, 278.63f, 48f, 272f)
      curveTo(48f, 265.37f, 42.63f, 260f, 36f, 260f)
      horizontalLineTo(231f)
      moveTo(24f, 236f)
      lineTo(228f, 236f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 240f, 248f)
      lineTo(240f, 248f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 228f, 260f)
      lineTo(24f, 260f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 248f)
      lineTo(12f, 248f)
      arcTo(12f, 12f, 0f, isMoreThanHalf = false, isPositiveArc = true, 24f, 236f)
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
      val startX = 66f
      val endX = startX + 140f
      moveTo(startX, 272f)

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
}

@VectorComposable
@Composable
private fun Camper(
  infiniteTransition: InfiniteTransition,
) {
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
    name = "Camper",
    translationY = camperTranslationY,
  ) {
    Path(fill = SolidColor(MaterialTheme.colorScheme.inversePrimary)) {
      moveTo(141.5f, 245.25f)
      horizontalLineTo(92.75f)
      curveTo(88.61f, 245.25f, 85.25f, 241.89f, 85.25f, 237.75f)
      verticalLineTo(185.25f)
      curveTo(85.25f, 181.11f, 88.61f, 177.75f, 92.75f, 177.75f)
      horizontalLineTo(165.88f)
      curveTo(172.09f, 177.75f, 177.13f, 182.79f, 177.13f, 189f)
      curveTo(177.13f, 192.11f, 174.61f, 194.63f, 171.5f, 194.63f)
      horizontalLineTo(154.63f)
      curveTo(149.45f, 194.63f, 145.25f, 198.82f, 145.25f, 204f)
      verticalLineTo(241.5f)
      curveTo(145.25f, 243.57f, 143.57f, 245.25f, 141.5f, 245.25f)
      close()
    }
    Path(fill = SolidColor(lightPrimary)) {
      moveTo(141.5f, 245.25f)
      horizontalLineTo(92.75f)
      curveTo(88.61f, 245.25f, 85.25f, 241.89f, 85.25f, 237.75f)
      verticalLineTo(230.25f)
      horizontalLineTo(145.25f)
      verticalLineTo(241.5f)
      curveTo(145.25f, 243.57f, 143.57f, 245.25f, 141.5f, 245.25f)
      close()
    }
    Path(fill = SolidColor(MaterialTheme.colorScheme.secondary)) {
      moveTo(149f, 245.25f)
      horizontalLineTo(179f)
      curveTo(181.07f, 245.25f, 182.75f, 243.57f, 182.75f, 241.5f)
      verticalLineTo(221.09f)
      curveTo(182.75f, 219.71f, 182.49f, 218.34f, 182f, 217.05f)
      lineTo(174.76f, 198.23f)
      curveTo(173.93f, 196.06f, 171.84f, 194.63f, 169.51f, 194.63f)
      horizontalLineTo(149f)
      curveTo(146.93f, 194.63f, 145.25f, 196.3f, 145.25f, 198.38f)
      verticalLineTo(241.5f)
      curveTo(145.25f, 243.57f, 146.93f, 245.25f, 149f, 245.25f)
      close()
    }
    Path(fill = SolidColor(darkPrimary)) {
      moveTo(115.25f, 194.63f)
      horizontalLineTo(135.88f)
      curveTo(137.95f, 194.63f, 139.63f, 196.3f, 139.63f, 198.38f)
      verticalLineTo(211.5f)
      curveTo(139.63f, 213.57f, 137.95f, 215.25f, 135.88f, 215.25f)
      horizontalLineTo(115.25f)
      curveTo(113.18f, 215.25f, 111.5f, 213.57f, 111.5f, 211.5f)
      verticalLineTo(198.38f)
      curveTo(111.5f, 196.3f, 113.18f, 194.63f, 115.25f, 194.63f)
      close()
    }
    Path(fill = SolidColor(darkPrimary)) {
      moveTo(98.38f, 194.63f)
      horizontalLineTo(102.13f)
      curveTo(104.2f, 194.63f, 105.88f, 196.3f, 105.88f, 198.38f)
      verticalLineTo(204f)
      curveTo(105.88f, 206.07f, 104.2f, 207.75f, 102.13f, 207.75f)
      horizontalLineTo(98.38f)
      curveTo(96.3f, 207.75f, 94.63f, 206.07f, 94.63f, 204f)
      verticalLineTo(198.38f)
      curveTo(94.63f, 196.3f, 96.3f, 194.63f, 98.38f, 194.63f)
      close()
    }
    Path(fill = SolidColor(darkPrimary)) {
      moveTo(148.06f, 183.38f)
      horizontalLineTo(164.94f)
      curveTo(166.49f, 183.38f, 167.75f, 184.63f, 167.75f, 186.19f)
      curveTo(167.75f, 187.74f, 166.49f, 189f, 164.94f, 189f)
      horizontalLineTo(148.06f)
      curveTo(146.51f, 189f, 145.25f, 187.74f, 145.25f, 186.19f)
      curveTo(145.25f, 184.63f, 146.51f, 183.38f, 148.06f, 183.38f)
      close()
    }
    Path(fill = SolidColor(lightPrimary)) {
      moveTo(156.5f, 219f)
      horizontalLineTo(168.99f)
      curveTo(172.03f, 219f, 174.15f, 215.98f, 173.11f, 213.12f)
      lineTo(168.65f, 202.72f)
      curveTo(168.11f, 201.24f, 166.7f, 200.25f, 165.12f, 200.25f)
      horizontalLineTo(156.5f)
      curveTo(154.43f, 200.25f, 152.75f, 201.93f, 152.75f, 204f)
      verticalLineTo(215.25f)
      curveTo(152.75f, 217.32f, 154.43f, 219f, 156.5f, 219f)
      close()
    }
    Path(
      fill = SolidColor(Color.Black),
      fillAlpha = 0.15f,
      strokeAlpha = 0.15f,
    ) {
      moveTo(182.75f, 221.08f)
      verticalLineTo(241.5f)
      curveTo(182.75f, 243.56f, 181.06f, 245.25f, 179f, 245.25f)
      horizontalLineTo(154.63f)
      curveTo(154.63f, 240.71f, 157.85f, 236.93f, 162.13f, 236.06f)
      lineTo(171.52f, 235.89f)
      curveTo(172.55f, 235.88f, 173.38f, 235.05f, 173.38f, 234.02f)
      verticalLineTo(228.38f)
      curveTo(173.38f, 223.26f, 177.48f, 219.09f, 182.56f, 219.02f)
      curveTo(182.68f, 219.69f, 182.75f, 220.39f, 182.75f, 221.08f)
      close()
    }
    Path(
      fill = SolidColor(Color.White),
      fillAlpha = 0.3f,
      strokeAlpha = 0.3f,
    ) {
      moveTo(124.63f, 177.75f)
      curveTo(124.63f, 182.93f, 120.43f, 187.13f, 115.25f, 187.13f)
      horizontalLineTo(94.63f)
      verticalLineTo(207.75f)
      curveTo(94.63f, 212.93f, 90.43f, 217.13f, 85.25f, 217.13f)
      verticalLineTo(185.25f)
      curveTo(85.25f, 181.11f, 88.61f, 177.75f, 92.75f, 177.75f)
      horizontalLineTo(124.63f)
      close()
    }
    Path(fill = SolidColor(Color.White)) {
      moveTo(91.81f, 196.5f)
      curveTo(90.26f, 196.5f, 89f, 195.24f, 89f, 193.69f)
      verticalLineTo(188.06f)
      curveTo(89f, 184.44f, 91.94f, 181.5f, 95.56f, 181.5f)
      horizontalLineTo(101.19f)
      curveTo(102.74f, 181.5f, 104f, 182.76f, 104f, 184.31f)
      curveTo(104f, 185.87f, 102.74f, 187.13f, 101.19f, 187.13f)
      horizontalLineTo(95.56f)
      curveTo(95.05f, 187.13f, 94.63f, 187.54f, 94.63f, 188.06f)
      verticalLineTo(193.69f)
      curveTo(94.63f, 195.24f, 93.36f, 196.5f, 91.81f, 196.5f)
      close()
    }
  }

  Group(
    name = "Tires",
  ) {
    Path(fill = SolidColor(darkPrimary)) {
      moveTo(164f, 256.5f)
      curveTo(170.21f, 256.5f, 175.25f, 251.46f, 175.25f, 245.25f)
      curveTo(175.25f, 239.04f, 170.21f, 234f, 164f, 234f)
      curveTo(157.79f, 234f, 152.75f, 239.04f, 152.75f, 245.25f)
      curveTo(152.75f, 251.46f, 157.79f, 256.5f, 164f, 256.5f)
      close()
    }
    Path(fill = SolidColor(lightPrimary)) {
      moveTo(164f, 250.88f)
      curveTo(167.11f, 250.88f, 169.63f, 248.36f, 169.63f, 245.25f)
      curveTo(169.63f, 242.14f, 167.11f, 239.63f, 164f, 239.63f)
      curveTo(160.89f, 239.63f, 158.38f, 242.14f, 158.38f, 245.25f)
      curveTo(158.38f, 248.36f, 160.89f, 250.88f, 164f, 250.88f)
      close()
    }

    Path(fill = SolidColor(darkPrimary)) {
      moveTo(107.75f, 256.5f)
      curveTo(113.96f, 256.5f, 119f, 251.46f, 119f, 245.25f)
      curveTo(119f, 239.04f, 113.96f, 234f, 107.75f, 234f)
      curveTo(101.54f, 234f, 96.5f, 239.04f, 96.5f, 245.25f)
      curveTo(96.5f, 251.46f, 101.54f, 256.5f, 107.75f, 256.5f)
      close()
    }
    Path(fill = SolidColor(lightPrimary)) {
      moveTo(107.75f, 250.88f)
      curveTo(110.86f, 250.88f, 113.38f, 248.36f, 113.38f, 245.25f)
      curveTo(113.38f, 242.14f, 110.86f, 239.63f, 107.75f, 239.63f)
      curveTo(104.64f, 239.63f, 102.13f, 242.14f, 102.13f, 245.25f)
      curveTo(102.13f, 248.36f, 104.64f, 250.88f, 107.75f, 250.88f)
      close()
    }
  }
}

@VectorComposable
@Composable
fun Path(
  name: String = DefaultPathName,
  fill: Brush? = null,
  fillAlpha: Float = 1.0f,
  stroke: Brush? = null,
  strokeAlpha: Float = 1.0f,
  strokeLineWidth: Float = DefaultStrokeLineWidth,
  strokeLineCap: StrokeCap = DefaultStrokeLineCap,
  strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
  strokeLineMiter: Float = DefaultStrokeLineMiter,
  pathFillType: PathFillType = DefaultFillType,
  pathBuilder: PathBuilder.() -> Unit,
) {
  Path(
    name = name,
    fill = fill,
    fillAlpha = fillAlpha,
    stroke = stroke,
    strokeAlpha = strokeAlpha,
    strokeLineWidth = strokeLineWidth,
    strokeLineCap = strokeLineCap,
    strokeLineJoin = strokeLineJoin,
    strokeLineMiter = strokeLineMiter,
    pathFillType = pathFillType,
    pathData = PathData(pathBuilder),
  )
}
