// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.api.AppTheme

internal val IconSize = 48.dp
private const val ColumnCount = 5
private val IconSpacing = 8.dp
private val GridPadding = 8.dp
private val GridWidth = IconSize * ColumnCount + IconSpacing * (ColumnCount - 1) + GridPadding * 2

@Composable
fun IconPicker(
  icon: AppTheme.Icon,
  onIconClick: (AppTheme.Icon) -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  Box(
    modifier = modifier,
  ) {
    Image(
      icon.icon(),
      contentDescription = "AppTheme Icon",
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable {
          isExpanded = true
        }
        .size(IconSize),
    )

    DropdownMenu(
      expanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      offset = DpOffset(-GridPadding, -(IconSize + GridPadding)),
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      shape = MaterialTheme.shapes.medium,
    ) {
      FlowRow(
        maxItemsInEachRow = ColumnCount,
        horizontalArrangement = Arrangement.spacedBy(IconSpacing, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(IconSpacing),
        modifier = Modifier
          .width(GridWidth),
      ) {
        AppTheme.Icon.entries.forEach { ico ->
          Image(
            ico.icon(),
            contentDescription = null,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                onIconClick(ico)
                isExpanded = false
              }
              .size(IconSize),
          )
        }
      }
    }
  }
}
