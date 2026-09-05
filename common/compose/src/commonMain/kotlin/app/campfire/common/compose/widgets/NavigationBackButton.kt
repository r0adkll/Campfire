// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowBack
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.action_back
import campfire.common.compose.generated.resources.action_close
import org.jetbrains.compose.resources.stringResource

/**
 * The top app bar's leading navigation action: a back arrow in a stacked navigation flow, or a
 * close cross when the screen sits in the supporting (detail) pane on wide windows, where there is
 * nothing behind it to go back to.
 * The label follows the glyph for tooltips and accessibility.
 */
@Composable
fun NavigationBackButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val inDetailPane = LocalContentLayout.current == ContentLayout.Supporting
  val label = stringResource(if (inDetailPane) Res.string.action_close else Res.string.action_back)
  IconButtonTooltip(text = label, modifier = modifier) {
    IconButton(onClick = onClick) {
      Icon(
        if (inDetailPane) CampfireIcons.Rounded.Close else CampfireIcons.Rounded.ArrowBack,
        contentDescription = label,
      )
    }
  }
}
