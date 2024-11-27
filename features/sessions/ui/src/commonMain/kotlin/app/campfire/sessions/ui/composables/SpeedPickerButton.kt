package app.campfire.sessions.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SpeedPickerButton(
  speed: (Float),
  speeds: List<Float>,
  onSpeedPicked: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(
      onClick = { isExpanded = true },
    ) {
      Icon(Icons.Rounded.Speed, contentDescription = null)
    }

    val optionAlpha by animateFloatAsState(if (isExpanded) 1f else 0f)

    RadialMenu(
      expanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      content = {
        Box(
          modifier = Modifier
            .align(Alignment.Center)
            .clip(CircleShape)
            .size(64.dp)
            .background(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape,
            )
            .clickable { isExpanded = false },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Rounded.Speed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    ) {
      speeds.forEach { s ->
        val isSelected = s == speed
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .graphicsLayer {
              alpha = optionAlpha
            }
            .clickable {
              onSpeedPicked(s)
              isExpanded = false
            }
            .selected(isSelected),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${s}x",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) {
              MaterialTheme.colorScheme.onPrimary
            } else {
              MaterialTheme.colorScheme.onSurface
            }
          )
        }
      }
    }
  }
}

@Composable
private fun SpeedMenuItem(
  speed: Float,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = {
      Text(
        text = "${speed}x",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
      )
    },
    onClick = onClick,
    modifier = modifier,
  )
}
