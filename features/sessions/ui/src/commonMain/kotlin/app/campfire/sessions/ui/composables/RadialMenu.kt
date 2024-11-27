package app.campfire.sessions.ui.composables

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun RadialMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  diameter: Dp = MenuDiameter,
  startingAngleDeg: Double = DefaultStartingAngleDeg,
  spreadAngleDeg: Double = DefaultSpreadAngleDeg,
  optionPadding: Dp = DefaultOptionPadding,
  offset: DpOffset = DpOffset.Zero,
  properties: PopupProperties = PopupProperties(clippingEnabled = false),
  content: @Composable BoxScope.() -> Unit,
  optionContent: @Composable PieScope.() -> Unit,
) {
  val expandedState = remember { MutableTransitionState(false) }
  expandedState.targetState = expanded

  val popupSize by animateDpAsState(
    targetValue = if (expanded) diameter else MenuDiameterCollapsed,
  )
  val popupElevation by animateDpAsState(
    targetValue = if (expanded) 8.dp else 0.dp,
  )

  if (expandedState.currentState || (expandedState.targetState || popupSize > MenuDiameterCollapsed)) {
    val density = LocalDensity.current
    val popupPositionProvider = remember(offset, density) {
      object : PopupPositionProvider {
        override fun calculatePosition(
          anchorBounds: IntRect,
          windowSize: IntSize,
          layoutDirection: LayoutDirection,
          popupContentSize: IntSize,
        ): IntOffset {
          return anchorBounds.center - (popupContentSize.div(2)).let { IntOffset(it.width, it.height) }
        }
      }
    }

    var focusManager: FocusManager? by mutableStateOf(null)
    var inputModeManager: InputModeManager? by mutableStateOf(null)
    Popup(
      onDismissRequest = onDismissRequest,
      popupPositionProvider = popupPositionProvider,
      properties = properties,
    ) {
      focusManager = LocalFocusManager.current
      inputModeManager = LocalInputModeManager.current

      Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier.size(popupSize),
          shadowElevation = popupElevation,
          shape = CircleShape,
          content = {
            Box(
              modifier = Modifier.fillMaxSize(),
            ) {
              PieLayout(
                modifier = Modifier.align(Alignment.Center),
                startingAngleDeg = startingAngleDeg,
                spreadAngleDeg = spreadAngleDeg,
                optionPadding = optionPadding,
                content = optionContent,
              )
              content()
            }
          },
        )
      }
    }
  }
}

internal val DefaultStartingAngleDeg = -190.0
internal val DefaultSpreadAngleDeg = 200.0
internal val DefaultOptionPadding = 32.dp
internal val MenuDiameter = 200.dp
internal val MenuDiameterCollapsed = 0.dp

private class Selected(val isSelected: Boolean) : ParentDataModifier {

  override fun Density.modifyParentData(parentData: Any?) = this@Selected
}

interface PieScope {

  fun Modifier.selected(selected: Boolean): Modifier
}

private class PieScopeImpl : PieScope {

  override fun Modifier.selected(selected: Boolean): Modifier {
    return this.then(Selected(selected))
  }
}

@Composable
internal fun PieLayout(
  modifier: Modifier = Modifier,
  startingAngleDeg: Double = DefaultStartingAngleDeg,
  spreadAngleDeg: Double = DefaultSpreadAngleDeg,
  optionPadding: Dp = DefaultOptionPadding,
  highlightColor: Color = MaterialTheme.colorScheme.primary,
  content: @Composable PieScope.() -> Unit,
) {
  val scope = remember { PieScopeImpl() }
  Box(
    modifier = modifier,
  ) {
    var numOptions by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf(-1) }

    Canvas(
      modifier = Modifier.fillMaxSize(),
    ) {
      if (numOptions > 0 && selectedOption != -1) {
        val anglePer = spreadAngleDeg / numOptions
        val angle = startingAngleDeg + (selectedOption * anglePer)
//          val length = (size.maxDimension.fastRoundToInt() / 2) - optionPadding.roundToPx()
//          val coordinates = getCoordinates(angle, length) +
//            IntOffset(center.x.fastRoundToInt(), center.y.fastRoundToInt())

        drawArc(
          color = highlightColor,
          startAngle = angle.toFloat(),
          sweepAngle = anglePer.toFloat(),
          useCenter = true,
        )
      }
    }

    Layout(
      content = {
        scope.content()
      },
    ) { measurables, constraints ->
      val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
      val size = max(constraints.maxWidth, constraints.maxHeight)
      val center = IntOffset(constraints.maxWidth / 2, constraints.maxHeight / 2)

      val placeables = measurables.map { it.measure(looseConstraints) }
      val anglePer = spreadAngleDeg / placeables.size

      layout(size, size) {
        numOptions = placeables.size
        for (i in placeables.indices) {
          val placeable = placeables[i]

          val selected = (placeable.parentData as? Selected) ?: Selected(false)
          if (selected.isSelected) {
            selectedOption = i
          }

          val angle = startingAngleDeg + (i * anglePer) + (anglePer / 2)
          val length = (size / 2) - optionPadding.roundToPx()

          val coordinates = getCoordinates(angle, length) + center
          placeable.place(
            coordinates.x - placeable.width / 2,
            coordinates.y - placeable.height / 2,
          )
        }
      }
    }
  }
}

@Composable
private fun PieHighlighter(
  modifier: Modifier = Modifier,
  startingAngleDeg: Double = DefaultStartingAngleDeg,
  spreadAngleDeg: Double = DefaultSpreadAngleDeg,
  optionPadding: Dp = DefaultOptionPadding,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  Canvas(
    modifier = Modifier.fillMaxSize()
  ) {

  }
}

private fun degreesToRadians(degrees: Double): Double {
  return degrees * PI / 180.0
}

private fun getCoordinates(angleDeg: Double, length: Int): IntOffset {
  val angleRadians = degreesToRadians(angleDeg)
  val x = length * cos(angleRadians)
  val y = length * sin(angleRadians)
  return IntOffset(x.fastRoundToInt(), y.fastRoundToInt())
}
