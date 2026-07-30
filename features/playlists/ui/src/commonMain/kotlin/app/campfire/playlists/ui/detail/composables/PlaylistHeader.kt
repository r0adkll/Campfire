// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.core.model.Playlist
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaylistHeader(
  description: String?,
  items: List<Playlist.Item.Expanded>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    if (description != null) {
      Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      Spacer(Modifier.height(16.dp))
    }

    Row(
      modifier = Modifier
        .height(48.dp)
        .padding(
          horizontal = 16.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val itemLabel = if (items.size == 1) "Item" else "Items"
      Text(
        text = "${items.size} $itemLabel",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )

      Spacer(Modifier.weight(1f))

      Icon(
        Icons.Outlined.Schedule,
        contentDescription = "Total duration",
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = items.fold(Duration.ZERO) { acc, item ->
          // Podcast entries reflect the chosen episode's duration; book entries reflect
          // the whole library item.
          val itemDuration = item.episode?.duration ?: item.libraryItem.media.duration
          acc + itemDuration
        }.thresholdReadoutFormat(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
