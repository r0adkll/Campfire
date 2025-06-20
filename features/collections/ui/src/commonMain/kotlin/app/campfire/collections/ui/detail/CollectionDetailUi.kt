package app.campfire.collections.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import app.campfire.collections.ui.detail.bottomsheet.showEditCollectionBottomSheet
import app.campfire.common.compose.extensions.plus
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemCard
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.CollectionDetailScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
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

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(state.collection?.name ?: screen.collectionName) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(
            onClick = { state.eventSink(CollectionDetailUiEvent.Back) },
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
        actions = {
          IconButton(
            onClick = {
              showDeleteConfirmation = true
            },
          ) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        },
      )
    },
    floatingActionButton = {
      val isExpanded by remember {
        derivedStateOf {
          gridState.firstVisibleItemIndex == 0 &&
            gridState.firstVisibleItemScrollOffset < 50
        }
      }

      val overlayHost = LocalOverlayHost.current
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
    },
    floatingActionButtonPosition = FabPosition.End,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (state.collectionContentState) {
      CollectionContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      CollectionContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_collection_detail_message),
        modifier = Modifier.padding(paddingValues),
      )

      is CollectionContentState.Loaded -> LoadedState(
        description = state.collection?.description,
        items = state.collectionContentState.items,
        onLibraryItemClick = { state.eventSink(CollectionDetailUiEvent.LibraryItemClick(it)) },
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
  description: String?,
  items: List<LibraryItem>,
  onLibraryItemClick: (LibraryItem) -> Unit,
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

    items(
      items = items,
      key = { it.id },
    ) { item ->
      LibraryItemCard(
        item = item,
        modifier = Modifier.clickable {
          onLibraryItemClick(item)
        },
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
