// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.find.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.EpisodeListItemDefaults
import app.campfire.podcasts.api.RemotePodcastEpisode
import app.campfire.podcasts.ui.find.FeedEpisodeRow

@Composable
internal fun EpisodeFeedList(
  rows: List<FeedEpisodeRow>,
  selectedEnclosureUrls: Set<String>,
  onRowClick: (RemotePodcastEpisode) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  listState: LazyListState = rememberLazyListState(),
) {
  LazyColumn(
    state = listState,
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(2.dp),
    modifier = modifier.fillMaxSize(),
  ) {
    items(
      count = rows.size,
      key = { index -> rows[index].episode.enclosureUrl },
    ) { index ->
      val isFirst = index == 0
      val isLast = index == rows.lastIndex

      val row = rows[index]
      FeedEpisodeRowItem(
        row = row,
        isSelected = row.episode.enclosureUrl in selectedEnclosureUrls,
        onClick = { onRowClick(row.episode) },
        shape = when {
          isFirst && isLast -> EpisodeListItemDefaults.singleItemShape()
          isFirst -> EpisodeListItemDefaults.topItemShape()
          isLast -> EpisodeListItemDefaults.bottomItemShape()
          else -> EpisodeListItemDefaults.middleItemShape()
        },
      )
    }

    item {
      Spacer(Modifier.navigationBarsPadding())
    }
  }
}
