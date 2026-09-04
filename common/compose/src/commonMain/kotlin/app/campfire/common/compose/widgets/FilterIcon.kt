// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FilterAlt

@Composable
fun FilterIcon(
  isEmpty: Boolean,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
) {
  Icon(
    if (isEmpty) CampfireIcons.Rounded.FilterAlt else CampfireIcons.Rounded.FilterAlt,
    tint = if (isEmpty) LocalContentColor.current else MaterialTheme.colorScheme.secondary,
    contentDescription = contentDescription ?: "Filter the list of cards",
    modifier = modifier,
  )
}
