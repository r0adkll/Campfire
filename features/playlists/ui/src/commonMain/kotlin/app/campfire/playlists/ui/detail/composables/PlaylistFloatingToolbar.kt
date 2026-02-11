package app.campfire.playlists.ui.detail.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.SwapCalls

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaylistFloatingToolbar(
  isReordering: Boolean,
  onPlayAllClick: () -> Unit,
  onEditClick: () -> Unit,
  onReorderChange: (Boolean) -> Unit,
  onDownloadClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
  expanded: Boolean = true,
) {
  HorizontalFloatingToolbar(
    expanded = true,
    floatingActionButton = {
      TooltipBox(
        positionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(
          TooltipAnchorPosition.Above,
        ),
        tooltip = { CampfireTooltip("Play all items in the playlist") },
        state = rememberTooltipState(),
      ) {
        FloatingToolbarDefaults.StandardFloatingActionButton(
          onClick = onPlayAllClick,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null)
        }
      }
    },
    floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.Start,
//        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
    modifier = modifier,
  ) {
    ToolbarButton(
      Icons.Rounded.Download,
      onClick = onDownloadClick,
      expanded = expanded,
      contentDescription = "Download playlist",
    )
    ToolbarButton(
      Icons.Rounded.Edit,
      onClick = onEditClick,
      expanded = expanded,
      contentDescription = "Edit playlist name and description",
    )
    ToggleToolbarButton(
      checked = isReordering,
      onCheckedChange = onReorderChange,
      icon = CampfireIcons.Rounded.SwapCalls,
      expanded = expanded,
      contentDescription = "Re-order playlist items",
    )
    ToolbarButton(
      Icons.Rounded.Delete,
      onClick = onDeleteClick,
      expanded = expanded,
      contentDescription = "Delete playlist",
      tint = MaterialTheme.colorScheme.error,
    )
  }
}

@Composable
internal fun ToolbarButton(
  icon: ImageVector,
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  expanded: Boolean = false,
  tint: Color = LocalContentColor.current,
) {
  TooltipBox(
    positionProvider =
    TooltipDefaults.rememberTooltipPositionProvider(
      TooltipAnchorPosition.Above,
    ),
    tooltip = { CampfireTooltip(contentDescription) },
    state = rememberTooltipState(),
    modifier = modifier,
  ) {
    IconButton(
      onClick = onClick,
      Modifier.focusProperties { canFocus = expanded },
    ) {
      Icon(
        icon,
        contentDescription = contentDescription,
        tint = tint,
      )
    }
  }
}

@Composable
internal fun ToggleToolbarButton(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  icon: ImageVector,
  contentDescription: String,
  modifier: Modifier = Modifier,
  expanded: Boolean = false,
) {
  TooltipBox(
    positionProvider =
    TooltipDefaults.rememberTooltipPositionProvider(
      TooltipAnchorPosition.Above,
    ),
    tooltip = { CampfireTooltip(contentDescription) },
    state = rememberTooltipState(),
    modifier = modifier,
  ) {
    IconToggleButton(
      checked = checked,
      onCheckedChange = onCheckedChange,
      Modifier.focusProperties { canFocus = expanded },
    ) {
      Icon(icon, contentDescription = contentDescription)
    }
  }
}

@Composable
private fun TooltipScope.CampfireTooltip(
  text: String,
  modifier: Modifier = Modifier,
) {
  PlainTooltip(
    caretShape = TooltipDefaults.caretShape(),
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(
        horizontal = 4.dp,
        vertical = 2.dp,
      ),
    )
  }
}
