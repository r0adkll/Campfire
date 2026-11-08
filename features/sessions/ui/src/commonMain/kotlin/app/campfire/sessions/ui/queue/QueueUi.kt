// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.queue

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.screens.QueueScreen
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.expanded.composables.ClearQueueButton
import app.campfire.sessions.ui.playback.expanded.composables.QueueContent
import app.campfire.sessions.ui.playback.expanded.composables.QueueItemContent
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_play_queue
import campfire.features.sessions.ui.generated.resources.queue_empty_message
import campfire.features.sessions.ui.generated.resources.queue_header_now_playing
import campfire.features.sessions.ui.generated.resources.queue_screen_title
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(QueueScreen::class, UserScope::class)
@Composable
fun QueueUi(
  state: QueueScreenUiState,
  modifier: Modifier = Modifier,
) {
  val queue = state.queueState.queue
  val canPlayQueue = state.nowPlaying == null && queue.isNotEmpty()

  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.queue_screen_title)) },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(QueueScreenUiEvent.Back) })
        },
        actions = {
          if (queue.isNotEmpty()) {
            ClearQueueButton(
              onConfirmClick = { state.queueState.eventSink(QueueUiEvent.ClearQueue) },
            )
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
    floatingActionButton = {
      if (canPlayQueue) {
        val label = stringResource(Res.string.action_play_queue)
        SmallExtendedFloatingActionButton(
          text = { Text(label) },
          icon = { Icon(CampfireIcons.Rounded.PlayArrow, contentDescription = null) },
          onClick = { state.eventSink(QueueScreenUiEvent.PlayQueue) },
          // The extended FAB doesn't merge its label into the button, which would leave
          // TalkBack announcing an unnamed button
          modifier = Modifier.semantics { contentDescription = label },
        )
      }
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    if (queue.isEmpty() && state.nowPlaying == null) {
      EmptyState(
        message = stringResource(Res.string.queue_empty_message),
        modifier = Modifier.padding(paddingValues),
      )
    } else {
      QueueContent(
        queue = queue,
        onItemClick = { state.queueState.eventSink(QueueUiEvent.QueueItemClick(it)) },
        onRemoveItem = { state.queueState.eventSink(QueueUiEvent.RemoveQueueItem(it)) },
        onReorderItem = { from, to -> state.queueState.eventSink(QueueUiEvent.ReorderItem(from, to)) },
        onReorderStopped = { state.queueState.eventSink(QueueUiEvent.ReorderStopped) },
        contentPadding = paddingValues + PaddingValues(bottom = if (canPlayQueue) FabClearance else 16.dp),
        header = {
          state.nowPlaying?.let { entry ->
            nowPlayingSection(
              entry = entry,
              onClick = { state.eventSink(QueueScreenUiEvent.OpenItem(entry)) },
            )
          }
          if (queue.isEmpty()) {
            item(key = "empty-queue") {
              Text(
                text = stringResource(Res.string.queue_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
              )
            }
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

private fun LazyListScope.nowPlayingSection(
  entry: QueuedEntry,
  onClick: () -> Unit,
) {
  item(key = "now-playing-header") {
    MetadataHeader(title = stringResource(Res.string.queue_header_now_playing))
  }
  item(key = "now-playing") {
    QueueItemContent(
      entry = entry,
      onClick = onClick,
    )
  }
}

private val FabClearance = 88.dp
