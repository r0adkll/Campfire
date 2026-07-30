// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmarks
import app.campfire.common.compose.icons.rounded.Description
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_bookmarks
import campfire.features.sessions.ui.generated.resources.action_chapters
import campfire.features.sessions.ui.generated.resources.action_description
import campfire.features.sessions.ui.generated.resources.action_history
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ActionRow(
  onBookmarksClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  timerContent: @Composable () -> Unit,
  onChapterListClick: () -> Unit,
  showChapters: Boolean,
  onDescriptionClick: () -> Unit,
  showDescription: Boolean,
  onHistoryClick: () -> Unit,
  showHistory: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    ActionContent(
      onBookmarksClick = onBookmarksClick,
      speedContent = speedContent,
      timerContent = timerContent,
      onChapterListClick = onChapterListClick,
      showChapters = showChapters,
      onDescriptionClick = onDescriptionClick,
      showDescription = showDescription,
      onHistoryClick = onHistoryClick,
      showHistory = showHistory,
      actionModifier = Modifier.weight(1f),
    )
  }
}

@Composable
internal fun ActionColumn(
  onBookmarksClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  timerContent: @Composable () -> Unit,
  onChapterListClick: () -> Unit,
  showChapters: Boolean,
  onDescriptionClick: () -> Unit,
  showDescription: Boolean,
  onHistoryClick: () -> Unit,
  showHistory: Boolean,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .width(72.dp),
    verticalArrangement = Arrangement.SpaceEvenly,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ActionContent(
      onBookmarksClick = onBookmarksClick,
      speedContent = speedContent,
      timerContent = timerContent,
      onChapterListClick = onChapterListClick,
      showChapters = showChapters,
      onDescriptionClick = onDescriptionClick,
      showDescription = showDescription,
      onHistoryClick = onHistoryClick,
      showHistory = showHistory,
      actionModifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ActionContent(
  onBookmarksClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  timerContent: @Composable () -> Unit,
  onChapterListClick: () -> Unit,
  showChapters: Boolean,
  onDescriptionClick: () -> Unit,
  showDescription: Boolean,
  onHistoryClick: () -> Unit,
  showHistory: Boolean,
  actionModifier: Modifier = Modifier,
) {
  Box(
    modifier = actionModifier,
    contentAlignment = Alignment.Center,
  ) {
    val bookmarksLabel = stringResource(Res.string.action_bookmarks)
    IconButtonTooltip(text = bookmarksLabel) {
      IconButton(
        onClick = onBookmarksClick,
      ) {
        Icon(Icons.Rounded.Bookmarks, contentDescription = bookmarksLabel)
      }
    }
  }

  Box(
    modifier = actionModifier,
    contentAlignment = Alignment.Center,
  ) {
    speedContent()
  }

  Box(
    modifier = actionModifier,
    contentAlignment = Alignment.Center,
  ) {
    timerContent()
  }

  if (showChapters) {
    Box(
      modifier = actionModifier,
      contentAlignment = Alignment.Center,
    ) {
      val chaptersLabel = stringResource(Res.string.action_chapters)
      IconButtonTooltip(text = chaptersLabel) {
        IconButton(
          onClick = onChapterListClick,
        ) {
          Icon(Icons.AutoMirrored.Rounded.List, contentDescription = chaptersLabel)
        }
      }
    }
  }

  if (showDescription) {
    Box(
      modifier = actionModifier,
      contentAlignment = Alignment.Center,
    ) {
      val descriptionLabel = stringResource(Res.string.action_description)
      IconButtonTooltip(text = descriptionLabel) {
        IconButton(
          onClick = onDescriptionClick,
        ) {
          Icon(CampfireIcons.Rounded.Description, contentDescription = descriptionLabel)
        }
      }
    }
  }

  if (showHistory) {
    Box(
      modifier = actionModifier,
      contentAlignment = Alignment.Center,
    ) {
      val historyLabel = stringResource(Res.string.action_history)
      IconButtonTooltip(text = historyLabel) {
        IconButton(
          onClick = onHistoryClick,
        ) {
          Icon(Icons.Rounded.History, contentDescription = historyLabel)
        }
      }
    }
  }
}
