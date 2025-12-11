package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.ui.theming.ui.builder.ContrastLevel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ContrastPicker(
  level: ContrastLevel,
  onLevelClick: (ContrastLevel) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    val modifiers = listOf(Modifier.weight(1f), Modifier.weight(1f), Modifier.weight(1f))
    ContrastLevel.entries.forEachIndexed { index, contrastLevel ->
      OutlinedToggleButton(
        checked = contrastLevel == level,
        onCheckedChange = {
          onLevelClick(contrastLevel)
        },
        shapes =
        when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          ContrastLevel.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
          checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = modifiers[index],
      ) {
        Icon(
          when (contrastLevel) {
            ContrastLevel.Normal -> Icons.Rounded.BrightnessLow
            ContrastLevel.Medium -> Icons.Rounded.BrightnessMedium
            ContrastLevel.High -> Icons.Rounded.BrightnessHigh
          },
          contentDescription = null,
          modifier = Modifier.size(ToggleButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
        Text(
          text = when (contrastLevel) {
            ContrastLevel.Normal -> "Normal"
            ContrastLevel.Medium -> "Medium"
            ContrastLevel.High -> "High"
          },
        )
      }
    }
  }
}
