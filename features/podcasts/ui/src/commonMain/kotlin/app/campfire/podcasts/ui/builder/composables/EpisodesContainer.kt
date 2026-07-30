// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val EpisodesContainerColor
  @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

internal val EpisodesPadding = 16.dp

@Composable
internal fun EpisodesContainer(
  modifier: Modifier = Modifier,
  containerColor: Color = EpisodesContainerColor,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .background(containerColor)
      .padding(
        horizontal = EpisodesPadding,
      ),
  ) {
    content()
  }
}
