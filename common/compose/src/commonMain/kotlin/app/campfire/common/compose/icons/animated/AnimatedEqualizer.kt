package app.campfire.common.compose.icons.animated

import androidx.compose.animation.core.EaseInOut
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

internal const val AnimatedScaleDuration = 5000

val AnimatedEqualizerPainter
  @Composable get() = rememberVectorPainter(
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 960f,
    viewportHeight = 960f,
    autoMirror = true,
  ) { _, _ ->
    val transition = rememberInfiniteTransition()

    val bar1Scale by transition.animateFloat(
      initialValue = 1f,
      targetValue = 0.25f,
      animationSpec = infiniteRepeatable(
        tween(AnimatedScaleDuration, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse,
      ),
    )
    Group(
      name = "bar1",
      pivotY = 800f,
      scaleY = bar1Scale,
    ) {
      Path(
        pathData = PathData {
          moveTo(240f, 800f)
          quadToRelative(-33f, 0f, -56.5f, -23.5f)
          reflectiveQuadTo(160f, 720f)
          verticalLineToRelative(-160f)
          quadToRelative(0f, -33f, 23.5f, -56.5f)
          reflectiveQuadTo(240f, 480f)
          quadToRelative(33f, 0f, 56.5f, 23.5f)
          reflectiveQuadTo(320f, 560f)
          verticalLineToRelative(160f)
          quadToRelative(0f, 33f, -23.5f, 56.5f)
          reflectiveQuadTo(240f, 800f)
          close()
        },
        fill = SolidColor(Color.Black),
      )
    }

    val bar2Scale by transition.animateFloat(
      initialValue = 1f,
      targetValue = 0.25f,
      animationSpec = infiniteRepeatable(
        tween(
          AnimatedScaleDuration,
          easing = EaseInOut,
          delayMillis = 100,
        ),
        repeatMode = RepeatMode.Reverse,
      ),
    )
    Group(
      name = "bar2",
      pivotY = 800f,
      scaleY = bar2Scale,
    ) {
      Path(
        pathData = PathData {
          moveTo(480f, 800f)
          quadToRelative(-33f, 0f, -56.5f, -23.5f)
          reflectiveQuadTo(400f, 720f)
          verticalLineToRelative(-480f)
          quadToRelative(0f, -33f, 23.5f, -56.5f)
          reflectiveQuadTo(480f, 160f)
          quadToRelative(33f, 0f, 56.5f, 23.5f)
          reflectiveQuadTo(560f, 240f)
          verticalLineToRelative(480f)
          quadToRelative(0f, 33f, -23.5f, 56.5f)
          reflectiveQuadTo(480f, 800f)
          close()
        },
        fill = SolidColor(Color.Black),
      )
    }

    val bar3Scale by transition.animateFloat(
      initialValue = 1f,
      targetValue = 0.25f,
      animationSpec = infiniteRepeatable(
        tween(
          AnimatedScaleDuration,
          easing = EaseInOut,
          delayMillis = 50,
        ),
        repeatMode = RepeatMode.Reverse,
      ),
    )
    Group(
      name = "bar3",
      pivotY = 800f,
      scaleY = bar3Scale,
    ) {
      Path(
        pathData = PathData {
          moveTo(720f, 800f)
          quadToRelative(-33f, 0f, -56.5f, -23.5f)
          reflectiveQuadTo(640f, 720f)
          verticalLineToRelative(-280f)
          quadToRelative(0f, -33f, 23.5f, -56.5f)
          reflectiveQuadTo(720f, 360f)
          quadToRelative(33f, 0f, 56.5f, 23.5f)
          reflectiveQuadTo(800f, 440f)
          verticalLineToRelative(280f)
          quadToRelative(0f, 33f, -23.5f, 56.5f)
          reflectiveQuadTo(720f, 800f)
          close()
        },
        fill = SolidColor(Color.Black),
      )
    }
  }
