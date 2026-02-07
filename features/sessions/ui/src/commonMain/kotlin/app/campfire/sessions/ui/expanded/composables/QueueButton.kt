package app.campfire.sessions.ui.expanded.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_queue
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun QueueButton(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = ButtonDefaults.MediumContainerHeight,
) {
  OutlinedToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    shapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors = ToggleButtonDefaults.outlinedToggleButtonColors(
//      checkedContainerColor = MaterialTheme.colorScheme.secondary,
//      checkedContentColor = MaterialTheme.colorScheme.onSecondary,
    ),
    contentPadding = ButtonDefaults.contentPaddingFor(buttonSize),
    modifier = modifier
      .heightIn(buttonSize),
  ) {
    Icon(
      Icons.AutoMirrored.Rounded.QueueMusic,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonSize)),
    )
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(buttonSize)))
    Text(
      text = stringResource(Res.string.action_queue),
      style = ButtonDefaults.textStyleFor(buttonSize),
    )
  }
}
