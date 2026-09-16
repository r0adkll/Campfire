// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DockToBottom
import app.campfire.common.compose.icons.rounded.OpenInNew
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_open_mini_player
import campfire.features.sessions.ui.generated.resources.action_return_to_window
import org.jetbrains.compose.resources.stringResource

/**
 * Breaks the player out into the mini window, or brings it back. Driven by plain values so the
 * bars can render it in tests; [HostedMiniPlayerAction] wires it to the ambient host.
 */
@Composable
fun MiniPlayerAction(
  isOpen: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val label = stringResource(
    if (isOpen) Res.string.action_return_to_window else Res.string.action_open_mini_player,
  )
  IconButtonTooltip(text = label, modifier = modifier) {
    IconButton(onClick = onClick, enabled = enabled) {
      Icon(
        imageVector = if (isOpen) CampfireIcons.Rounded.DockToBottom else CampfireIcons.Rounded.OpenInNew,
        contentDescription = label,
      )
    }
  }
}

/**
 * [MiniPlayerAction] bound to [LocalMiniPlayerHost]; renders nothing where there is no host.
 *
 * [onOpened] fires once the player has been handed to the window, so the sheet this button sits
 * in can collapse rather than leave a second full player behind the new one.
 */
@Composable
internal fun HostedMiniPlayerAction(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onOpened: () -> Unit = {},
) {
  val host = LocalMiniPlayerHost.current ?: return
  MiniPlayerAction(
    isOpen = host.isOpen,
    onClick = {
      if (host.isOpen) {
        host.close()
      } else {
        host.open()
        onOpened()
      }
    },
    enabled = enabled,
    modifier = modifier,
  )
}
