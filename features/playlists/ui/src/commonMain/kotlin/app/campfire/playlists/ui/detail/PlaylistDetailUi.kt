// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.permission.PermissionState
import app.campfire.common.compose.permission.rememberPostNotificationPermissionState
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.ItemCollectionSharedTransitionKey
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.dialog.ConfirmDownloadDialog
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Playlist
import app.campfire.core.offline.OfflineStatus
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.playlists.ui.detail.composables.PlaylistFloatingToolbar
import app.campfire.playlists.ui.detail.composables.PlaylistHeader
import app.campfire.playlists.ui.detail.composables.PlaylistListItem
import app.campfire.playlists.ui.sheets.EditPlaylistModel
import app.campfire.playlists.ui.sheets.showEditPlaylistBottomSheet
import campfire.features.playlists.ui.generated.resources.Res
import campfire.features.playlists.ui.generated.resources.action_back
import campfire.features.playlists.ui.generated.resources.dialog_confirm_delete_action_cancel
import campfire.features.playlists.ui.generated.resources.dialog_confirm_delete_action_delete
import campfire.features.playlists.ui.generated.resources.dialog_confirm_delete_message
import campfire.features.playlists.ui.generated.resources.dialog_confirm_delete_title
import campfire.features.playlists.ui.generated.resources.empty_playlist_detail_message
import campfire.features.playlists.ui.generated.resources.error_playlist_detail_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(PlaylistDetailScreen::class, UserScope::class)
@Composable
fun PlaylistDetail(
  screen: PlaylistDetailScreen,
  state: PlaylistDetailUiState,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val scope = rememberCoroutineScope()
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  var showDeleteConfirmation by remember { mutableStateOf(false) }
  if (showDeleteConfirmation) {
    ConfirmDeleteDialog(
      playlistName = state.name,
      onDismiss = { showDeleteConfirmation = false },
      onConfirm = {
        state.eventSink(PlaylistDetailUiEvent.Delete)
        showDeleteConfirmation = false
      },
    )
  }

  var showConfirmDownloadDialog by remember { mutableStateOf(false) }
  var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }
  val postNotificationPermissionState = rememberPostNotificationPermissionState {
    if (it) {
      state.eventSink(PlaylistDetailUiEvent.DownloadAll(doNotShowDownloadConfirmationAgain))
      showConfirmDownloadDialog = false
    }
  }

  if (showConfirmDownloadDialog) {
    val downloadConfirmItems = remember(state.playlistItems) {
      state.playlistItems.map { it.libraryItem }.distinctBy { it.id }
    }
    ConfirmDownloadDialog(
      downloadConfirmItems,
      onConfirm = { doNotShowAgain ->
        if (postNotificationPermissionState is PermissionState.Granted) {
          state.eventSink(PlaylistDetailUiEvent.DownloadAll(doNotShowAgain))
          showConfirmDownloadDialog = false
        } else {
          doNotShowDownloadConfirmationAgain = doNotShowAgain
          postNotificationPermissionState.launchPermissionRequest()
        }
      },
      onDismissRequest = { showConfirmDownloadDialog = false },
    )
  }

  var isReordering by remember { mutableStateOf(false) }

  val overlayHost = LocalOverlayHost.current
  Scaffold(
    topBar = {
      PlaylistTopBar(
        name = state.name,
        scrollBehavior = scrollBehavior,
        onBack = { state.eventSink(PlaylistDetailUiEvent.Back) },
      )
    },
    floatingActionButton = {
      PlaylistFloatingToolbar(
        onPlayAllClick = {
          state.eventSink(PlaylistDetailUiEvent.PlayAll)
        },
        onEditClick = {
          val playlist = state.playlistState.dataOrNull ?: return@PlaylistFloatingToolbar
          scope.launch {
            overlayHost.showEditPlaylistBottomSheet(
              model = EditPlaylistModel.Existing(playlist),
            )
          }
        },
        onDownloadClick = {
          if (state.showConfirmDownloadDialog) {
            showConfirmDownloadDialog = true
          } else {
            state.eventSink(PlaylistDetailUiEvent.DownloadAll())
          }
        },
        isReordering = isReordering,
        onReorderChange = { isReordering = it },
        onDeleteClick = { showDeleteConfirmation = true },
      )
    },
    floatingActionButtonPosition = FabPosition.Center,
    modifier = modifier
      .nestedScroll(scrollBehavior.nestedScrollConnection)
      .sharedBounds(
        sharedContentState = rememberSharedContentState(
          ItemCollectionSharedTransitionKey(
            id = screen.playlistId,
            type = ItemCollectionSharedTransitionKey.ElementType.Bounds,
          ),
        ),
        animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        zIndexInOverlay = 0f,
      ),
    contentWindowInsets = CampfireWindowInsets.exclude(WindowInsets.navigationBars),
  ) { paddingValues ->
    when (state.playlistContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_playlist_detail_message),
        modifier = Modifier
          .fillMaxHeight()
          .padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedContent(
        name = state.name,
        description = state.description,
        items = state.playlistItems,
        onItemClick = { item ->
          state.eventSink(PlaylistDetailUiEvent.ItemClick(item))
        },
        onPlayClick = { item ->
          state.eventSink(PlaylistDetailUiEvent.PlayClick(item))
        },
        onRemove = { item ->
          state.eventSink(PlaylistDetailUiEvent.RemoveItem(item))
        },
        onReorderItem = { from, to ->
          state.reorderSink(from, to)
        },
        onReorderStopped = {
          state.eventSink(PlaylistDetailUiEvent.ReorderStopped)
        },
        offlineStateSelector = { itemId -> state.offlineStates[itemId].asWidgetStatus() },
        isPlayingSelector = { item ->
          val session = state.currentSession
          session != null &&
            session.libraryItem.id == item.libraryItemId &&
            session.episodeId == item.episodeId
        },
        contentPadding = paddingValues + PaddingValues(
          // 2 x 16dp (padding) + 56dp (toolbar)
          bottom = 104.dp,
        ),
        isReordering = isReordering,
        modifier = Modifier
          .fillMaxSize(),
      )
    }
  }
}

@Composable
private fun PlaylistTopBar(
  name: String,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CampfireTopAppBar(
    modifier = modifier,
    title = { Text(name) },
    scrollBehavior = scrollBehavior,
    windowInsets = WindowInsets(),
    contentPadding = WindowInsets.statusBars.asPaddingValues(),
    navigationIcon = {
      val backLabel = stringResource(Res.string.action_back)
      IconButtonTooltip(text = backLabel) {
        IconButton(
          onClick = onBack,
        ) {
          Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = backLabel,
          )
        }
      }
    },
  )
}

@Composable
private fun LoadedContent(
  name: String,
  description: String?,
  items: List<Playlist.Item.Expanded>,
  isReordering: Boolean,
  onItemClick: (Playlist.Item.Expanded) -> Unit,
  onPlayClick: (Playlist.Item.Expanded) -> Unit,
  onRemove: (Playlist.Item.Expanded) -> Unit,
  onReorderItem: suspend (fromKey: String, toKey: String) -> Unit,
  onReorderStopped: () -> Unit,
  offlineStateSelector: (LibraryItemId) -> OfflineStatus,
  isPlayingSelector: (Playlist.Item.Expanded) -> Boolean,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  state: LazyListState = rememberLazyListState(),
) {
  val haptics = LocalHapticFeedback.current
  val reorderableLazyListState = rememberReorderableLazyListState(state) { from, to ->
    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    onReorderItem(from.key as String, to.key as String)
  }
  LazyColumn(
    state = state,
    contentPadding = contentPadding,
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item("description") {
      PlaylistHeader(
        description = description,
        items = items,
      )
    }

    itemsIndexed(
      items = items,
      key = { _, item -> item.key },
    ) { index, item ->
      ReorderableItem(reorderableLazyListState, key = item.key) { isDragging ->
        val interactionSource = remember { MutableInteractionSource() }
        PlaylistListItem(
          item = item,
          sharedTransitionKey = item.key + name,
          sharedTransitionZIndex = (items.size - index) + 1f,
          offlineStatus = offlineStateSelector(item.libraryItem.id),
          isPlaying = isPlayingSelector(item),
          onClick = {
            onItemClick(item)
          },
          onPlayClick = {
            onPlayClick(item)
          },
          onRemove = {
            onRemove(item)
          },
          isDragging = isDragging,
          isReordering = isReordering,
          interactionSource = interactionSource,
          handleModifier = Modifier
            .draggableHandle(
              enabled = isReordering,
              interactionSource = interactionSource,
              onDragStarted = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              },
              onDragStopped = {
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onReorderStopped()
              },
            ),
        )
      }
    }
  }

  if (items.isEmpty()) {
    EmptyState(
      message = stringResource(Res.string.empty_playlist_detail_message),
      modifier = Modifier
        .padding(vertical = 32.dp),
    )
  }
}

@Composable
private fun ConfirmDeleteDialog(
  playlistName: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    modifier = modifier,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
    text = {
      Text(
        buildAnnotatedString {
          append(stringResource(Res.string.dialog_confirm_delete_message))
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(" \"${playlistName}\"")
          }
          append("?")
        },
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(
          text = stringResource(Res.string.dialog_confirm_delete_action_delete),
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.dialog_confirm_delete_action_cancel))
      }
    },
  )
}
