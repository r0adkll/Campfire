package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastRoundToInt
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.icon
import app.campfire.core.model.Tent
import kotlin.math.max

@Composable
fun CampsiteIcon(
  tent: Tent,
  hasFire: Boolean,
  modifier: Modifier = Modifier,
) {
  Layout(
    content = {
      Image(
        tent.icon,
        contentDescription = null,
      )

      if (hasFire) {
        Image(
          CampfireIcons.Campfire,
          contentDescription = null,
        )
      }
    },
    modifier = modifier,
  ) { measurables, constraints ->
    val tentMeasurable = measurables[0]
    val tentPlaceable = tentMeasurable.measure(constraints)

    val fireMeasurable = measurables.getOrNull(1)
    val tentSize = max(tentPlaceable.width, tentPlaceable.height)
    val fireSize = (tentSize * 0.53f).fastRoundToInt()
    val fireConstraints = constraints.copy(
      minWidth = fireSize,
      minHeight = fireSize,
      maxWidth = fireSize,
      maxHeight = fireSize,
    )
    val firePlaceable = fireMeasurable?.measure(fireConstraints)

    layout(tentPlaceable.width, tentPlaceable.height) {
      tentPlaceable.place(0, 0, zIndex = 0f)
      firePlaceable?.let {
        val positionX = (tentPlaceable.width * 0.08f).fastRoundToInt()
        val positionY = (tentPlaceable.height * 0.42f).fastRoundToInt()
        it.place(positionX, positionY, zIndex = 1f)
      }
    }
  }
}
