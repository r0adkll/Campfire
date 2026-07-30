// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.action_find_episodes
import campfire.features.libraries.ui.generated.resources.header_episodes
import org.jetbrains.compose.resources.stringResource

class EpisodeHeaderSlot(
  private val episodeCount: Int = 0,
  private val showFindEpisodes: Boolean = false,
) : ContentSlot {

  override val id: String = "episode_header"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.extraLarge.copy(
        bottomStart = ZeroCornerSize,
        bottomEnd = ZeroCornerSize,
      ),
      color = ChapterContainerColor,
    ) {
      Column {
        Spacer(Modifier.height(8.dp))

        // Header
        MetadataHeader(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(stringResource(Res.string.header_episodes))
              if (episodeCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(
                      horizontal = 6.dp,
                      vertical = 6.dp,
                    ),
                ) {
                  Text(
                    text = episodeCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
            }
          },
          trailingContent = if (showFindEpisodes) {
            {
              val findEpisodesLabel = stringResource(Res.string.action_find_episodes)
              IconButtonTooltip(text = findEpisodesLabel) {
                IconButton(
                  onClick = { eventSink(LibraryItemUiEvent.FindEpisodes) },
                ) {
                  Icon(
                    Icons.Rounded.Search,
                    contentDescription = findEpisodesLabel,
                  )
                }
              }
            }
          } else {
            null
          },
          modifier = Modifier
            .heightIn(min = 48.dp)
            .padding(
              horizontal = 24.dp,
            ),
        )

        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

@Preview
@Composable
fun EpisodeHeaderSlotPreview() {
  CampfireTheme {
    EpisodeHeaderSlot().Content(Modifier) {}
  }
}
