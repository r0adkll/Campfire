// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.ui.detail.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.MetadataHeader

@Composable
internal fun AuthorDetailHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  MetadataHeader(
    title = title,
    modifier = modifier,
  )
}
