package app.campfire.libraries.ui.detail.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.r0adkll.swatchbuckler.compose.Swatch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SwatchToolbar(
  swatch: Swatch,
  onColorClicked: (Color) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AnimatedVisibility(
      visible = expanded,
      enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
      exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
    ) {
      Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      ) {
        Row(
          modifier = Modifier
            .padding(
              horizontal = 12.dp,
              vertical = 6.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val colorOptionSize by transition.animateDp { state ->
            if (state == EnterExitState.Visible) DefaultColorOptionSize else 8.dp
          }

          ColorOptionButton(
            color = swatch.dominant,
            onClick = {
              onColorClicked(swatch.dominant)
            },
            size = colorOptionSize,
            modifier = Modifier.animateEnterExit(
              enter = expandIn(expandFrom = Alignment.Center, clip = false),
              exit = shrinkOut(shrinkTowards = Alignment.Center, clip = false),
            ),
          )

          swatch.vibrant.forEach { c ->
            ColorOptionButton(
              color = c,
              onClick = {
                onColorClicked(c)
              },
              size = colorOptionSize,
              modifier = Modifier.animateEnterExit(
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center, clip = false),
                exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center, clip = false),
              ),
            )
          }
        }
      }
    }

    IconToggleButton(
      checked = expanded,
      onCheckedChange = { expanded = it },
      shapes = IconToggleButtonShapes(
        shape = CircleShape,
        pressedShape = MaterialTheme.shapes.medium,
      ),
      colors = IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ),
    ) {
      Icon(
        if (expanded) Icons.Rounded.Close else Icons.Rounded.Palette,
        contentDescription = null,
      )
    }
  }
}

private val DefaultColorOptionSize = 32.dp

@Composable
private fun ColorOptionButton(
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = DefaultColorOptionSize,
) {
  Box(
    modifier
      .clip(CircleShape)
      .clickable(onClick = onClick)
      .size(size)
      .background(color)
      .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
  )
}
