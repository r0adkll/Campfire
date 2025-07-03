package app.campfire.common.compose.icons.animated

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

val AnimatedTimerPainter
  @Composable get() = rememberVectorPainter(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ) { _, _ ->
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        tween(5000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
    )

    // Top Button
    Path(
      pathData = PathData {
        moveTo(400f, 120f)
        quadToRelative(-17f, 0f, -28.5f, -11.5f)
        reflectiveQuadTo(360f, 80f)
        quadToRelative(0f, -17f, 11.5f, -28.5f)
        reflectiveQuadTo(400f, 40f)
        horizontalLineToRelative(160f)
        quadToRelative(17f, 0f, 28.5f, 11.5f)
        reflectiveQuadTo(600f, 80f)
        quadToRelative(0f, 17f, -11.5f, 28.5f)
        reflectiveQuadTo(560f, 120f)
        lineTo(400f, 120f)
        close()
      },
      fill = SolidColor(Color.Black),
    )

    Group(
      name = "needle",
      rotation = rotation,
      pivotX = 480f,
      pivotY = 520f,
    ) {
      Path(
        pathData = PathData {
          // Needle
          moveTo(480f, 560f)
          quadToRelative(17f, 0f, 28.5f, -11.5f)
          reflectiveQuadTo(520f, 520f)
          verticalLineToRelative(-160f)
          quadToRelative(0f, -17f, -11.5f, -28.5f)
          reflectiveQuadTo(480f, 320f)
          quadToRelative(-17f, 0f, -28.5f, 11.5f)
          reflectiveQuadTo(440f, 360f)
          verticalLineToRelative(160f)
          quadToRelative(0f, 17f, 11.5f, 28.5f)
          reflectiveQuadTo(480f, 560f)
          close()
        },
        fill = SolidColor(Color.Black),
      )
    }

    Path(
      pathData = PathData {
        moveTo(480f, 880f)
        quadToRelative(-74f, 0f, -139.5f, -28.5f)
        reflectiveQuadTo(226f, 774f)
        quadToRelative(-49f, -49f, -77.5f, -114.5f)
        reflectiveQuadTo(120f, 520f)
        quadToRelative(0f, -74f, 28.5f, -139.5f)
        reflectiveQuadTo(226f, 266f)
        quadToRelative(49f, -49f, 114.5f, -77.5f)
        reflectiveQuadTo(480f, 160f)
        quadToRelative(62f, 0f, 119f, 20f)
        reflectiveQuadToRelative(107f, 58f)
        lineToRelative(28f, -28f)
        quadToRelative(11f, -11f, 28f, -11f)
        reflectiveQuadToRelative(28f, 11f)
        quadToRelative(11f, 11f, 11f, 28f)
        reflectiveQuadToRelative(-11f, 28f)
        lineToRelative(-28f, 28f)
        quadToRelative(38f, 50f, 58f, 107f)
        reflectiveQuadToRelative(20f, 119f)
        quadToRelative(0f, 74f, -28.5f, 139.5f)
        reflectiveQuadTo(734f, 774f)
        quadToRelative(-49f, 49f, -114.5f, 77.5f)
        reflectiveQuadTo(480f, 880f)
        close()
        moveTo(480f, 800f)
        quadToRelative(116f, 0f, 198f, -82f)
        reflectiveQuadToRelative(82f, -198f)
        quadToRelative(0f, -116f, -82f, -198f)
        reflectiveQuadToRelative(-198f, -82f)
        quadToRelative(-116f, 0f, -198f, 82f)
        reflectiveQuadToRelative(-82f, 198f)
        quadToRelative(0f, 116f, 82f, 198f)
        reflectiveQuadToRelative(198f, 82f)
        close()
      },
      fill = SolidColor(Color.Black),
    )

    Path(
      pathData = PathData {
        moveTo(480f, 520f)
        close()
      },
      fill = SolidColor(Color.Black),
    )
  }
