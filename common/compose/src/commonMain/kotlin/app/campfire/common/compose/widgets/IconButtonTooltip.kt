package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Accessibility wrapper that pairs an icon-only button with the standardized Campfire plain
 * tooltip. Per Material 3 guidance, every `IconButton` (and its derivatives — `FilledIconButton`,
 * `OutlinedIconButton`, `FilledTonalIconButton`, `IconToggleButton`, etc.) should be wrapped with
 * a `TooltipBox` so the action's label is surfaced to long-press, hover, and assistive
 * technologies.
 *
 * Usage:
 * ```
 * IconButtonTooltip(text = stringResource(Res.string.edit_playlist)) {
 *   IconButton(onClick = onEditClick) {
 *     Icon(Icons.Rounded.Edit, contentDescription = stringResource(Res.string.edit_playlist))
 *   }
 * }
 * ```
 *
 * Callers should pass the same string to the inner `Icon`'s `contentDescription` so screen readers
 * announce the action when the button is focused, in addition to revealing the tooltip on
 * long-press / hover.
 *
 * @param text the action label, shown in the tooltip. Prefer a `stringResource(...)` lookup so the
 *   label is localizable.
 * @param modifier modifier applied to the underlying [TooltipBox] anchor.
 * @param position tooltip anchor position relative to the wrapped content. Defaults to
 *   [TooltipAnchorPosition.Above], matching Material's recommendation for top app bars and
 *   toolbars.
 * @param state state controller for the tooltip; override to drive visibility programmatically.
 * @param content the `IconButton` (or any derivative) to wrap.
 */
@Composable
fun IconButtonTooltip(
  text: String,
  modifier: Modifier = Modifier,
  position: TooltipAnchorPosition = TooltipAnchorPosition.Above,
  state: TooltipState = rememberTooltipState(),
  content: @Composable () -> Unit,
) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
    tooltip = {
      PlainTooltip(
        caretShape = TooltipDefaults.caretShape(),
        shape = MaterialTheme.shapes.small,
      ) {
        Text(
          text = text,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
      }
    },
    state = state,
    modifier = modifier,
    content = content,
  )
}
