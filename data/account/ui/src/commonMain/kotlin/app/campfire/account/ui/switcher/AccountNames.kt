// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.ui.switcher

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AccountSwitch
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.data.account.ui.generated.resources.Res
import campfire.data.account.ui.generated.resources.action_switch_account
import org.jetbrains.compose.resources.stringResource

/**
 * The server and user names of an account switcher card, with the switch account button.
 *
 * When the server name fits on one line beside the button, both names stack and the button sits
 * beside them, centered. When it doesn't, the server name gets the full width (up to two lines) and
 * the button moves onto the user name line, taking no height of its own so that line stays as tall
 * as its text; the button's touch target reaches above and below it. Without a [userName] (while
 * the account loads) the user name line is left out when the server name fits beside the button,
 * and kept blank to hold the button when it doesn't.
 */
@Composable
internal fun AccountNames(
  serverName: String,
  userName: String?,
  serverNameStyle: TextStyle,
  userNameStyle: TextStyle,
  onSwitchAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier) {
    val measurer = rememberTextMeasurer()
    val besideWidth = constraints.maxWidth -
      with(LocalDensity.current) { (SwitchButtonSize + ButtonSpacing).roundToPx() }
    val fitsBeside = remember(measurer, serverName, serverNameStyle, besideWidth) {
      !measurer.measure(
        text = serverName,
        style = serverNameStyle,
        maxLines = 1,
        constraints = Constraints(maxWidth = besideWidth.coerceAtLeast(0)),
      ).hasVisualOverflow
    }

    if (fitsBeside) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(serverName, style = serverNameStyle, maxLines = 1)
          if (userName != null) {
            Spacer(Modifier.height(NameSpacing))
            Text(userName, style = userNameStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
        Spacer(Modifier.width(ButtonSpacing))
        SwitchAccountButton(onClick = onSwitchAccount)
      }
    } else {
      Column {
        Text(serverName, style = serverNameStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(NameSpacing))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = userName ?: " ",
            style = userNameStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(ButtonSpacing))
          SwitchAccountButton(
            onClick = onSwitchAccount,
            // Takes no height, so the user name line stays as tall as its text
            modifier = Modifier.layout { measurable, constraints ->
              val placeable = measurable.measure(constraints.copy(minHeight = 0))
              layout(placeable.width, 0) { placeable.place(0, -placeable.height / 2) }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun SwitchAccountButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val switchAccountLabel = stringResource(Res.string.action_switch_account)
  IconButtonTooltip(text = switchAccountLabel, modifier = modifier) {
    IconButton(onClick = onClick) {
      Icon(
        CampfireIcons.Rounded.AccountSwitch,
        contentDescription = switchAccountLabel,
      )
    }
  }
}

private val SwitchButtonSize = 48.dp
private val NameSpacing = 4.dp
private val ButtonSpacing = 8.dp
