package app.campfire.ui.theming.ui.ai.emptystate.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.graphics.shapes.RoundedPolygon
import com.r0adkll.cadence.game.components.Renderable
import com.r0adkll.cadence.game.components.Transform
import com.r0adkll.cadence.game.ecs.Entity
import com.r0adkll.cadence.game.ecs.World
import kotlin.random.Random

/**
 * The particle component that renders the [shape] at a given [size] and [alpha]
 * along with the [Transform] positional data.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ShapeParticle(
  val shape: RoundedPolygon,
  val alpha: Float,
  val size: Dp,
) : Renderable {

  @Composable
  override fun Content(entity: Entity, world: World) {
    val transform = world.getComponent<Transform>(entity)!!

    val infiniteTransition = rememberInfiniteTransition()
    val jitterEasing = EaseInOutSine

    val jitterXDirection = remember { if (Random.nextBoolean()) 1 else -1 }
    val offsetJitterXDuration = remember { Random.nextInt(800, 1200) * 2 }
    val offsetJitterXDist = with(LocalDensity.current) { 4.dp.toPx() }
    val offsetJitterX = infiniteTransition.animateFloat(
      initialValue = -offsetJitterXDist * jitterXDirection,
      targetValue = offsetJitterXDist * jitterXDirection,
      animationSpec = infiniteRepeatable(
        animation = tween(offsetJitterXDuration, easing = jitterEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(offsetJitterXDuration / 2, StartOffsetType.FastForward),
      ),
    )

    val jitterYDirection = remember { if (Random.nextBoolean()) 1 else -1 }
    val offsetJitterYDuration = remember { Random.nextInt(800, 1200) * 2 }
    val offsetJitterYDist = with(LocalDensity.current) { 4.dp.toPx() }
    val offsetJitterY = infiniteTransition.animateFloat(
      initialValue = -offsetJitterYDist * jitterYDirection,
      targetValue = offsetJitterXDist * jitterYDirection,
      animationSpec = infiniteRepeatable(
        animation = tween(offsetJitterYDuration, easing = jitterEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(offsetJitterYDuration / 2, StartOffsetType.FastForward),
      ),
    )

    val animatedRotation by animateFloatAsState(transform.rotation)

    val color = remember {
      Random.nextInt(3)
    }

    Box(
      Modifier
        .offset {
          transform.position.toInt() +
            IntOffset(offsetJitterX.value.fastRoundToInt(), offsetJitterY.value.fastRoundToInt())
        }
        .rotate(animatedRotation)
        .size(size)
        .alpha(alpha)
        .background(
          color = when (color) {
            0 -> MaterialTheme.colorScheme.tertiary
            1 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
          },
          shape = shape.toShape(),
        ),
    )
  }
}

fun Offset.toInt(): IntOffset = IntOffset(x.fastRoundToInt(), y.fastRoundToInt())
