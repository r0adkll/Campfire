package app.campfire.libraries.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.colorScheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaType
import app.campfire.core.model.User
import app.campfire.core.model.User.Permissions
import app.campfire.core.model.User.Type
import app.campfire.core.model.UserId
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.composables.DeletePodcastDialog
import app.campfire.libraries.ui.detail.composables.SwatchToolbar
import app.campfire.libraries.ui.detail.composables.slots.ChapterContainerColor
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsTitle
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.CoverImageSlot
import app.campfire.libraries.ui.detail.composables.slots.ExpressiveControlSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SpacerSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleSlot
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.cd_add_to_collection
import campfire.features.libraries.ui.generated.resources.cd_back_arrow
import campfire.features.libraries.ui.generated.resources.cd_more_actions
import campfire.features.libraries.ui.generated.resources.error_library_item_message
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.menu_item_delete_podcast
import campfire.features.libraries.ui.generated.resources.tags_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  screen: LibraryItemScreen,
  state: LibraryItemUiState,
  addToCollectionDialog: AddToCollectionDialog,
  modifier: Modifier = Modifier,
) {
  MaterialExpressiveTheme(
    colorScheme = state.theme?.colorScheme,
  ) {
    LibraryItemContent(
      screen = screen,
      state = state,
      addToCollectionDialog = addToCollectionDialog,
      modifier = modifier,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryItemContent(
  screen: LibraryItemScreen,
  state: LibraryItemUiState,
  addToCollectionDialog: AddToCollectionDialog,
  modifier: Modifier,
) = SharedElementTransitionScope {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  var showAddToCollectionDialog by remember { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val snackBarHost = remember { SnackbarHostState() }
  val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)

  state.errorMessage?.let { message ->
    LaunchedEffect(message) {
      snackBarHost.showSnackbar(message)
      state.eventSink(LibraryItemUiEvent.ClearError)
    }
  }

  val canDeletePodcast = state.user.canDeleteItems &&
    state.libraryItem?.mediaType == MediaType.Podcast
  Scaffold(
    containerColor = surfaceColor,
    snackbarHost = {
      SnackbarHost(snackBarHost) { data ->
        Snackbar(
          snackbarData = data,
          shape = MaterialTheme.shapes.medium,
        )
      }
    },
    topBar = {
      CampfireTopAppBar(
        title = {},
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          val backLabel = stringResource(Res.string.cd_back_arrow)
          IconButtonTooltip(text = backLabel) {
            IconButton(
              onClick = {
                state.eventSink(LibraryItemUiEvent.OnBack)
              },
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backLabel,
              )
            }
          }
        },
        actions = {
          AnimatedVisibility(
            visible = state.swatch != null,
            modifier = Modifier,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            SwatchToolbar(
              swatch = state.swatch!!,
              onColorClicked = {
                state.eventSink(LibraryItemUiEvent.SeedColorChange(it))
              },
            )
          }

          if (state.user.canEditCollections) {
            val addToCollectionLabel = stringResource(Res.string.cd_add_to_collection)
            IconButtonTooltip(text = addToCollectionLabel) {
              IconButton(
                onClick = {
                  Analytics.send(ActionEvent("add_to_collection", Click))
                  showAddToCollectionDialog = true
                },
              ) {
                Icon(
                  Icons.Rounded.LibraryAdd,
                  contentDescription = addToCollectionLabel,
                )
              }
            }
          }

          if (canDeletePodcast) {
            val moreActionsLabel = stringResource(Res.string.cd_more_actions)
            IconButtonTooltip(text = moreActionsLabel) {
              IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                  Icons.Rounded.MoreVert,
                  contentDescription = moreActionsLabel,
                )
              }
            }
            DropdownMenu(
              expanded = showOverflowMenu,
              onDismissRequest = { showOverflowMenu = false },
              shape = MaterialTheme.shapes.medium,
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    text = stringResource(Res.string.menu_item_delete_podcast),
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                leadingIcon = {
                  Icon(
                    Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showOverflowMenu = false
                  showDeleteDialog = true
                },
              )
            }
          }
        },
      )
    },
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState(
          LibraryItemSharedTransitionKey(
            id = screen.sharedTransitionKey,
            type = LibraryItemSharedTransitionKey.ElementType.Bounds,
          ),
        ),
        animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
      )
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (val contentState = state.contentState) {
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_item_message),
        modifier = Modifier.padding(paddingValues),
      )

      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      is LoadState.Loaded<out ContentUiState> -> {
        CompositionLocalProvider(
          LocalSnackBarHost provides snackBarHost,
        ) {
          LoadedState(
            slots = contentState.data.slots,
            contentPadding = paddingValues,
            modifier = modifier,
            eventSink = state.eventSink,
          )
        }
      }
    }
  }

  if (showAddToCollectionDialog) {
    addToCollectionDialog.Content(
      item = state.libraryItem!!,
      onDismiss = { showAddToCollectionDialog = false },
      modifier = Modifier,
    )
  }

  if (showDeleteDialog) {
    DeletePodcastDialog(
      onConfirm = { hardDelete ->
        Analytics.send(
          ActionEvent(
            "delete_podcast",
            Click,
            if (hardDelete) "hard" else "soft",
          ),
        )
        showDeleteDialog = false
        state.eventSink(LibraryItemUiEvent.DeleteItemClick(hardDelete))
      },
      onDismissRequest = { showDeleteDialog = false },
    )
  }
}

@Composable
private fun LoadedState(
  slots: List<ContentSlot>,
  eventSink: (LibraryItemUiEvent) -> Unit,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
      end = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
      top = contentPadding.calculateTopPadding(),
      bottom = 0.dp,
    ),
  ) {
    items(
      items = slots,
      key = { it.id },
      contentType = { it.contentType },
    ) { slot ->
      slot.Content(
        Modifier.animateItem(),
        eventSink,
      )
    }

    item {
      Box(
        modifier = Modifier
          .background(ChapterContainerColor)
          .fillMaxWidth()
          .height(contentPadding.calculateBottomPadding()),
      )
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(heightDp = 2200)
@Composable
fun LibraryItemPreview() = PreviewSharedElementTransitionLayout {
  CampfireTheme {
    CompositionLocalProvider(
      LocalWindowSizeClass provides calculateWindowSizeClass(),
      LocalContentLayout provides ContentLayout.Root,
    ) {
      val libraryItem = libraryItem()
      val mediaProgress = mediaProgress()
      val offlineDownload = OfflineDownload(
        libraryItemId = "",
        state = OfflineDownload.State.Completed,
        contentLength = 500L * 1024L * 1024L,
      )

      fun user(
        id: UserId,
        name: String = "Test User",
        selectedLibraryId: LibraryId = "test_library_id",
        type: Type = Type.Admin,
        isActive: Boolean = true,
        isLocked: Boolean = false,
        lastSeen: Long = 0L,
        createdAt: Long = 0L,
        permissions: Permissions = Permissions(
          download = true,
          update = true,
          delete = true,
          upload = true,
          accessAllLibraries = true,
          accessAllTags = true,
          accessExplicitContent = true,
        ),
        serverUrl: String = "https://test.url",
      ) = User(
        id = id,
        name = name,
        selectedLibraryId = selectedLibraryId,
        type = type,
        isActive = isActive,
        isLocked = isLocked,
        lastSeen = lastSeen,
        createdAt = createdAt,
        permissions = permissions,
        serverUrl = serverUrl,
      )

      val slotState = LibraryItemUiState(
        user = user("user_id"),
        libraryItem = libraryItem,
        contentState = LoadState.Loaded(
          data = ContentUiState(
            slots = listOf(
              CoverImageSlot("", "", ""),
              TitleSlot(libraryItem, ""),
              SpacerSlot.medium("progress_spacer"),
              ProgressSlot(false, 1f, mediaProgress, libraryItem),
              SpacerSlot.medium("control_spacer"),
              ExpressiveControlSlot(
                libraryItem = libraryItem,
                offlineDownload = offlineDownload,
                mediaProgress = mediaProgress,
                showConfirmDownloadDialogSetting = true,
                isCurrentSession = false,
                hasSession = false,
                isQueued = false,
                addToPlaylistDialog = AddToPlaylistDialog.NoOp,
              ),
              SpacerSlot.medium("summary_spacer"),
              SummarySlot(libraryItem.media.metadata.description!!),
              SpacerSlot.medium("series_spacer"),
              SeriesSlot(
                libraryItem = libraryItem,
                seriesBooks = listOf(
                  libraryItem(),
                  libraryItem(),
                  libraryItem(),
                  libraryItem(),
                ),
              ),
              SpacerSlot.medium("genres_spacer"),
              ChipsSlot(
                title = ChipsTitle(Res.plurals.genres_title, 2),
                chips = libraryItem.media.metadata.genres,
              ),
              SpacerSlot.medium("tags_spacer"),
              ChipsSlot(
                title = ChipsTitle(Res.plurals.tags_title, 2),
                chips = libraryItem.media.tags,
              ),
              SpacerSlot.large("chapters_spacer"),
              ChapterHeaderSlot(
                showTimeInBook = true,
              ),
              *libraryItem.media.chapters.map {
                ChapterSlot(
                  libraryItem = libraryItem,
                  chapter = it,
                  showTimeInBook = true,
                  mediaProgress = mediaProgress,
                )
              }.toTypedArray(),
            ),
            eventSink = {},
          ),
        ),
        eventSink = {},
      )

      LibraryItemContent(
        screen = LibraryItemScreen("itemId"),
        state = slotState,
        addToCollectionDialog = object : AddToCollectionDialog {
          @Composable
          override fun Content(item: LibraryItem, onDismiss: () -> Unit, modifier: Modifier) {
          }
        },
        modifier = Modifier,
      )
    }
  }
}
