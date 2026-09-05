// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.ui.switcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.coroutines.LoadState
import app.campfire.socket.SocketState
import app.campfire.ui.theming.api.AppTheme
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.action_switch_account
import campfire.data.account.ui.generated.resources.server_name_error
import campfire.data.account.ui.generated.resources.server_name_loading
import org.jetbrains.compose.resources.stringResource

/**
 * Width of the expanded switcher card. Wider than the rail's 220dp minimum would allow so a typical
 * server name fits beside the icon and the switch-account button; the rail grows to match.
 */
val RailAccountSwitcherWidth = 232.dp

private val CollapsedIconSize = 56.dp
private val ExpandedIconSize = 40.dp

/**
 * The account switcher shaped for the header of the desktop wide navigation rail. Collapsed it is
 * only the account icon; expanded it grows into a compact card with the server and user names, a
 * switch-account button, and the library picker. In both states tapping the icon calls [onToggle]
 * so the icon doubles as the rail's expand/collapse control.
 *
 * @param toggleLabel the accessible label for the icon, describing what [onToggle] will do
 * @param onSwitchAccount called with the presenter's event sink when the switch-account button is
 *   pressed, so the caller can show the account picker and feed the result back
 */
@Composable
fun RailAccountSwitcher(
  expanded: Boolean,
  toggleLabel: String,
  onToggle: () -> Unit,
  onSwitchAccount: (eventSink: (AccountSwitcherUiEvent) -> Unit) -> Unit,
  modifier: Modifier = Modifier,
  component: AccountSwitcherComponent = rememberComponent(),
) {
  val presenter = remember(component) { component.accountSwitcherPresenterFactory() }
  val state = presenter.present()
  RailAccountSwitcher(
    state = state,
    expanded = expanded,
    toggleLabel = toggleLabel,
    onToggle = onToggle,
    onSwitchAccount = { onSwitchAccount(state.eventSink) },
    modifier = modifier,
  )
}

@Composable
internal fun RailAccountSwitcher(
  state: AccountSwitcherUiState,
  expanded: Boolean,
  toggleLabel: String,
  onToggle: () -> Unit,
  onSwitchAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val serverName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.name
    LoadState.Loading -> stringResource(Res.string.server_name_loading)
    LoadState.Error -> stringResource(Res.string.server_name_error)
  }

  val userName = when (val currentAccount = state.currentAccount) {
    is LoadState.Loaded -> currentAccount.data.user.name
    else -> null
  }

  AnimatedContent(
    targetState = expanded,
    transitionSpec = {
      (fadeIn() togetherWith fadeOut()) using SizeTransform(clip = false)
    },
    modifier = modifier,
  ) { isExpanded ->
    if (isExpanded) {
      AccountCard(
        modifier = Modifier.width(RailAccountSwitcherWidth),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(
              start = 12.dp,
              top = 12.dp,
              end = 4.dp,
              bottom = 12.dp,
            ),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ToggleAccountIcon(
            appTheme = state.theme,
            socketState = state.socketState,
            size = ExpandedIconSize,
            indicatorSize = 8.dp,
            indicatorOffset = 3.dp,
            indicatorBorderColor = MaterialTheme.colorScheme.primaryContainer,
            label = toggleLabel,
            onClick = onToggle,
          )

          Spacer(Modifier.width(12.dp))

          Column(
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = serverName,
              style = MaterialTheme.typography.titleMedium.copy(fontFamily = PaytoneOneFontFamily),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            userName?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          val switchAccountLabel = stringResource(Res.string.action_switch_account)
          IconButtonTooltip(text = switchAccountLabel) {
            IconButton(onClick = onSwitchAccount) {
              Icon(
                CampfireIcons.Rounded.AccountSwitch,
                contentDescription = switchAccountLabel,
              )
            }
          }
        }

        if (state.libraryState != null) {
          LibraryPicker(
            state = state.libraryState,
            onLibraryClick = { library ->
              state.eventSink(AccountSwitcherUiEvent.SelectLibrary(library))
            },
            rowStyle = CompactLibraryRowStyle,
          )
        }
      }
    } else {
      ToggleAccountIcon(
        appTheme = state.theme,
        socketState = state.socketState,
        size = CollapsedIconSize,
        indicatorSize = 12.dp,
        indicatorOffset = 6.dp,
        indicatorBorderColor = MaterialTheme.colorScheme.surface,
        label = toggleLabel,
        onClick = onToggle,
      )
    }
  }
}

@Composable
private fun ToggleAccountIcon(
  appTheme: AppTheme,
  socketState: SocketState,
  size: Dp,
  indicatorSize: Dp,
  indicatorOffset: Dp,
  indicatorBorderColor: Color,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  IconButtonTooltip(text = label) {
    AccountIcon(
      appTheme = appTheme,
      socketState = socketState,
      size = size,
      indicatorSize = indicatorSize,
      indicatorOffset = indicatorOffset,
      indicatorBorderColor = indicatorBorderColor,
      modifier = modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick),
    )
  }
}
