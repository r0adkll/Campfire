// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.stats.ui.StatsUiModel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatsHeader(
  model: StatsUiModel.Header,
  modifier: Modifier = Modifier,
) {
  MetadataHeader(
    title = stringResource(model.title),
    modifier = modifier
      .heightIn(min = 56.dp)
      .padding(horizontal = 16.dp),
  )
}
