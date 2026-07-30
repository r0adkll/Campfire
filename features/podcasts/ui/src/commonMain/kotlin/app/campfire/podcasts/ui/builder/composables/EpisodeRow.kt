// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.common.compose.widgets.MetadataChip
import app.campfire.core.extensions.asDate
import app.campfire.podcasts.api.RemotePodcastEpisode

@Composable
internal fun EpisodeRow(
  episode: RemotePodcastEpisode,
  isSelected: Boolean,
  isFirst: Boolean,
  isLast: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  EpisodesContainer(
    modifier = modifier,
  ) {
    ElevatedCard(
      modifier = Modifier
        .fillMaxWidth(),
      shape = if (isFirst && !isLast) {
        EpisodeListItemDefaults.topItemShape()
      } else if (!isFirst && !isLast) {
        EpisodeListItemDefaults.middleItemShape()
      } else if (!isFirst && isLast) {
        EpisodeListItemDefaults.bottomItemShape()
      } else {
        EpisodeListItemDefaults.singleItemShape()
      },
      colors = CardDefaults.elevatedCardColors(
        containerColor = if (isSelected) {
          MaterialTheme.colorScheme.secondaryContainer
        } else {
          Color.Unspecified
        },
      ),
      elevation = CardDefaults.elevatedCardElevation(
        defaultElevation = if (isSelected) {
          6.dp // level 3
        } else {
          1.dp // level 1
        },
      ),
      onClick = onToggle,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
          horizontal = 16.dp,
          vertical = 16.dp,
        ),
      ) {
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = episode.title,
            style = MaterialTheme.typography.titleMediumEmphasized,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          episode.descriptionPlain?.let { desc ->
            Text(
              text = desc.trimStart(),
              style = MaterialTheme.typography.bodySmall,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }

          val parts = buildList {
            episode.publishedAtMillis?.asDate()?.relativeDayLabel?.let { add(it) }
            episode.duration?.thresholdReadoutFormat()?.let { add(it) }
            episode.episode?.takeIf { it.isNotBlank() }?.let { add("#$it") }
            episode.season?.takeIf { it.isNotBlank() }?.let { add("S$it") }
          }
          if (parts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))

            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              parts.forEach { part ->
                MetadataChip {
                  Text(part)
                }
              }
            }
          }
        }

        Spacer(Modifier.width(8.dp))

        Checkbox(
          checked = isSelected,
          onCheckedChange = null,
          modifier = Modifier.align(Alignment.Top),
        )
      }
    }

    if (!isLast) {
      Spacer(Modifier.height(2.dp))
    } else {
      Spacer(Modifier.height(48.dp))
    }
  }
}
