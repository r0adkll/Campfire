// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.home.api.model.ShelfIds
import app.campfire.home.ui.UiShelf
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.upcoming_shelf_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShelfHeader(
  shelf: UiShelf<*>,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .height(48.dp)
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      // The synthetic upcoming shelf is built outside composition, so its
      // label resolves here instead of from the server feed.
      text = if (shelf.id == ShelfIds.UpcomingReleases) {
        stringResource(Res.string.upcoming_shelf_title)
      } else {
        shelf.label
      },
      style = MaterialTheme.typography.labelLarge,
    )
  }
}
