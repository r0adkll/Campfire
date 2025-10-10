package app.campfire.collections.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.asWidgetStatus
import app.campfire.collections.ui.detail.bottomsheet.showEditCollectionBottomSheet
import app.campfire.collections.ui.detail.composables.CollectionDetailTopAppBar
import app.campfire.collections.ui.detail.composables.EditingTopAppBar
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.action_edit_collection
import campfire.features.collections.ui.generated.resources.dialog_confirm_delete_action_cancel
import campfire.features.collections.ui.generated.resources.dialog_confirm_delete_action_delete
import campfire.features.collections.ui.generated.resources.dialog_confirm_delete_message
import campfire.features.collections.ui.generated.resources.dialog_confirm_delete_title
import campfire.features.collections.ui.generated.resources.error_collection_detail_message
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@CircuitInject(CollectionDetailScreen::class, UserScope::class)
@Composable
fun CollectionDetail(
  screen: CollectionDetailScreen,
  state: CollectionDetailUiState,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  val gridState = rememberLazyGridState()

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  var isItemEditing by remember { mutableStateOf(false) }
  val selectedItems = remember { mutableStateListOf<LibraryItem>() }

  LaunchedEffect(isItemEditing, selectedItems.isEmpty()) {
    if (isItemEditing && selectedItems.isEmpty()) {
      isItemEditing = false
    }
  }

  Scaffold(
    topBar = {
      AnimatedContent(
        targetState = isItemEditing,
        transitionSpec = {
          if (targetState) {
            slideIntoContainer(
              towards = AnimatedContentTransitionScope.SlideDirection.Down,
            ) togetherWith fadeOut(targetAlpha = 0.5f)
          } else {
            fadeIn(initialAlpha = 0.5f) togetherWith
              slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Up,
              )
          }
        },
      ) { isEditing ->
        if (isEditing) {
          EditingTopAppBar(
            title = {
              Text(
                text = "${selectedItems.size} item${if (selectedItems.size > 1) "s" else ""}",
              )
            },
            actions = {
              IconButton(
                onClick = {
                  state.eventSink(CollectionDetailUiEvent.DeleteItems(selectedItems.toList()))
                  isItemEditing = false
                  selectedItems.clear()
                },
              ) {
                Icon(
                  Icons.Rounded.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            },
            onDismiss = {
              isItemEditing = false
              selectedItems.clear()
            },
            scrollBehavior = scrollBehavior,
          )
        } else {
          CollectionDetailTopAppBar(
            name = state.collection?.name ?: screen.collectionName,
            scrollBehavior = scrollBehavior,
            onBack = { state.eventSink(CollectionDetailUiEvent.Back) },
            onDelete = { showDeleteConfirmation = true },
          )
        }
      }
    },
    floatingActionButton = {
      val isExpanded by remember {
        derivedStateOf {
          gridState.firstVisibleItemIndex == 0 &&
            gridState.firstVisibleItemScrollOffset < 50
        }
      }

      val overlayHost = LocalOverlayHost.current
      AnimatedVisibility(
        visible = !isItemEditing,
      ) {
        ExtendedFloatingActionButton(
          onClick = {
            scope.launch {
              overlayHost.showEditCollectionBottomSheet(state.collection!!)
            }
          },
          text = { Text(stringResource(Res.string.action_edit_collection)) },
          icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          expanded = isExpanded,
        )
      }
    },
    floatingActionButtonPosition = FabPosition.End,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets
      .fluentIf(LocalContentLayout.current != ContentLayout.Supporting) {
        exclude(WindowInsets.navigationBars)
      },
  ) { paddingValues ->
    when (state.collectionContentState) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_collection_detail_message),
        modifier = Modifier.padding(paddingValues),
      )

      is LoadState.Loaded -> LoadedState(
        collectionName = screen.collectionName,
        description = state.collection?.description,
        isEditing = isItemEditing,
        selectedItems = selectedItems,
        items = state.collectionContentState.data,
        offlineStatus = { state.offlineStates[it].asWidgetStatus() },
        onLibraryItemClick = { item ->
          if (isItemEditing) {
            if (selectedItems.contains(item)) {
              selectedItems.remove(item)
            } else {
              selectedItems.add(item)
            }
          } else {
            state.eventSink(CollectionDetailUiEvent.LibraryItemClick(item))
          }
        },
        onLibraryItemLongClick = { item ->
          isItemEditing = true
          selectedItems.clear()
          selectedItems.add(item)
        },
        contentPadding = paddingValues,
        gridState = gridState,
      )
    }
  }

  if (showDeleteConfirmation) {
    ConfirmDeleteDialog(
      collectionName = state.collection?.name ?: "",
      onDismiss = { showDeleteConfirmation = false },
      onConfirm = {
        showDeleteConfirmation = false
        state.eventSink(CollectionDetailUiEvent.Delete)
      },
    )
  }
}

@Composable
private fun LoadedState(
  collectionName: String,
  description: String?,
  isEditing: Boolean,
  selectedItems: List<LibraryItem>,
  items: List<LibraryItem>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  onLibraryItemClick: (LibraryItem) -> Unit,
  onLibraryItemLongClick: (LibraryItem) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  gridState: LazyGridState = rememberLazyGridState(),
) {
  LaunchedEffect(description) {
    if (description != null && gridState.firstVisibleItemIndex > 0) {
      gridState.animateScrollToItem(0)
    }
  }

  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    state = gridState,
    modifier = modifier,
    contentPadding = contentPadding + PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (description != null) {
      item(
        key = "description",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        Text(
          text = description,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.padding(
            bottom = 8.dp,
          ),
        )
      }
    }

    itemsIndexed(
      items = items,
      key = { _, item -> item.id },
    ) { index, item ->
      LibraryItemCard(
        item = item,
        sharedTransitionKey = item.id + collectionName,
        sharedTransitionZIndex = -(index + 1f),
        offlineStatus = offlineStatus(item.id),
        isSelectable = isEditing,
        selected = selectedItems.contains(item),
        modifier = Modifier
          .combinedClickable(
            onClickLabel = "View item",
            onClick = { onLibraryItemClick(item) },
            onLongClickLabel = "Enter item edit mode",
            onLongClick = { onLibraryItemLongClick(item) },
          ),
      )
    }
  }
}

@Composable
private fun ConfirmDeleteDialog(
  collectionName: String,
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
            append(" \"${collectionName}\"")
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
