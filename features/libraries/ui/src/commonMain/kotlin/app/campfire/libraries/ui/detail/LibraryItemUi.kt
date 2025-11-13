package app.campfire.libraries.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.composables.slots.ChapterHeaderSlot
import app.campfire.libraries.ui.detail.composables.slots.ChapterSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsSlot
import app.campfire.libraries.ui.detail.composables.slots.ChipsTitle
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import app.campfire.libraries.ui.detail.composables.slots.ControlSlot
import app.campfire.libraries.ui.detail.composables.slots.CoverImageSlot
import app.campfire.libraries.ui.detail.composables.slots.OfflineStatusSlot
import app.campfire.libraries.ui.detail.composables.slots.ProgressSlot
import app.campfire.libraries.ui.detail.composables.slots.PublishedSlot
import app.campfire.libraries.ui.detail.composables.slots.SeriesSlot
import app.campfire.libraries.ui.detail.composables.slots.SpacerSlot
import app.campfire.libraries.ui.detail.composables.slots.SummarySlot
import app.campfire.libraries.ui.detail.composables.slots.TitleAndAuthorSlot
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.cd_add_to_collection
import campfire.features.libraries.ui.generated.resources.cd_back_arrow
import campfire.features.libraries.ui.generated.resources.error_library_item_message
import campfire.features.libraries.ui.generated.resources.genres_title
import campfire.features.libraries.ui.generated.resources.tags_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(heightDp = 1600)
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

      val slotState = LibraryItemUiState(
        libraryItem = libraryItem,
        contentState = LoadState.Loaded(
          data = listOf(
            CoverImageSlot("", "", ""),
            TitleAndAuthorSlot(libraryItem),
            SpacerSlot.medium("progress_spacer"),
            ProgressSlot(mediaProgress),
            SpacerSlot.medium("control_spacer"),
            ControlSlot(
              libraryItem = libraryItem,
              offlineDownload = offlineDownload,
              mediaProgress = mediaProgress,
              showConfirmDownloadDialogSetting = true,
            ),
            SpacerSlot.medium("offline_spacer"),
            OfflineStatusSlot(offlineDownload),
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
            SpacerSlot.medium("published_spacer"),
            PublishedSlot(
              publisher = libraryItem.media.metadata.publisher!!,
              publishedYear = libraryItem.media.metadata.publishedYear!!,
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
        ),
        showConfirmDownloadDialog = false,
        eventSink = {},
      )

      LibraryItem(
        state = slotState,
        addToCollectionDialog = object : AddToCollectionDialog {
          @Composable
          override fun Content(item: LibraryItem, onDismiss: () -> Unit, modifier: Modifier) {
          }
        },
      )
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  state: LibraryItemUiState,
  addToCollectionDialog: AddToCollectionDialog,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  var showAddToCollectionDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = {},
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(
            onClick = {
              state.eventSink(LibraryItemUiEvent.OnBack)
            },
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(Res.string.cd_back_arrow),
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              Analytics.send(ActionEvent("add_to_collection", Click))
              showAddToCollectionDialog = true
            },
          ) {
            Icon(
              Icons.Rounded.LibraryAdd,
              contentDescription = stringResource(Res.string.cd_add_to_collection),
            )
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (val contentState = state.contentState) {
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_item_message),
        modifier = Modifier.padding(paddingValues),
      )

      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      is LoadState.Loaded<out List<ContentSlot>> -> LoadedState(
        slots = contentState.data,
        contentPadding = paddingValues,
        modifier = modifier,
        eventSink = state.eventSink,
      )
    }
  }

  if (showAddToCollectionDialog) {
    addToCollectionDialog.Content(
      item = state.libraryItem!!,
      onDismiss = { showAddToCollectionDialog = false },
      modifier = Modifier,
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
    contentPadding = contentPadding,
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
  }
}
