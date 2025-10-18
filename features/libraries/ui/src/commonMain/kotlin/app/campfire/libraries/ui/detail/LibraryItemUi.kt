package app.campfire.libraries.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.collections.api.ui.AddToCollectionDialog
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BookRibbon
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.Platform
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.currentPlatform
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.libraries.ui.detail.composables.AuthorNarratorBar
import app.campfire.libraries.ui.detail.composables.ControlBar
import app.campfire.libraries.ui.detail.composables.DurationListItem
import app.campfire.libraries.ui.detail.composables.GenreChips
import app.campfire.libraries.ui.detail.composables.ItemDescription
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.composables.OfflineStatusCard
import app.campfire.libraries.ui.detail.composables.SeriesMetadata
import app.campfire.libraries.ui.detail.dialog.ConfirmDownloadDialog
import app.campfire.libraries.ui.detail.permission.PermissionState
import app.campfire.libraries.ui.detail.permission.rememberPostNotificationPermissionState
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.error_library_item_message
import campfire.features.libraries.ui.generated.resources.header_chapters
import campfire.features.libraries.ui.generated.resources.placeholder_book
import campfire.features.libraries.ui.generated.resources.unknown_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  screen: LibraryItemScreen,
  state: LibraryItemUiState,
  addToCollectionDialog: AddToCollectionDialog,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  var showAddToCollectionDialog by remember { mutableStateOf(false) }
  var showConfirmDownloadDialog by remember { mutableStateOf(false) }

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
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
          }
        },
        actions = {
          if (currentPlatform == Platform.ANDROID) {
            IconButton(
              onClick = {
                // TODO: Implement ChromeCast integration
              },
            ) {
              Icon(Icons.Rounded.Cast, contentDescription = null)
            }
          }
          IconButton(
            onClick = {
              Analytics.send(ActionEvent("add_to_collection", Click))
              showAddToCollectionDialog = true
            },
          ) {
            Icon(Icons.Rounded.LibraryAdd, contentDescription = null)
          }
          IconButton(
            onClick = {},
          ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null)
          }
        },
      )
    },
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = CampfireWindowInsets,
  ) { paddingValues ->
    when (val contentState = state.libraryItemContentState) {
      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_item_message),
        modifier = Modifier.padding(paddingValues),
      )

      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      is LoadState.Loaded<out LibraryItem> -> LoadedState(
        item = contentState.data,
        sharedTransitionKey = screen.sharedTransitionKey,
        seriesContentState = state.seriesContentState,
        mediaProgressState = state.mediaProgressState,
        offlineDownload = state.offlineDownloadState,
        showTimeInBook = state.showTimeInBook,
        modifier = modifier,
        contentPadding = paddingValues,
        onAuthorClick = {
          state.eventSink(LibraryItemUiEvent.AuthorClick(contentState.data))
        },
        onNarratorClick = {
          state.eventSink(LibraryItemUiEvent.NarratorClick(contentState.data))
        },
        onChapterClick = { chapter ->
          state.eventSink(LibraryItemUiEvent.ChapterClick(contentState.data, chapter))
        },
        onPlayClick = {
          state.eventSink(LibraryItemUiEvent.PlayClick(contentState.data))
        },
        onDownloadClick = {
          if (state.showConfirmDownloadDialog) {
            showConfirmDownloadDialog = true
          } else {
            state.eventSink(LibraryItemUiEvent.DownloadClick())
          }
        },
        onRemoveDownloadClick = {
          state.eventSink(LibraryItemUiEvent.RemoveDownloadClick)
        },
        onStopDownloadClick = {
          state.eventSink(LibraryItemUiEvent.StopDownloadClick)
        },
        onSeriesClick = {
          state.eventSink(LibraryItemUiEvent.SeriesClick(contentState.data))
        },
        onMarkFinished = {
          state.eventSink(LibraryItemUiEvent.MarkFinished(contentState.data))
        },
        onMarkNotFinished = {
          state.eventSink(LibraryItemUiEvent.MarkNotFinished(contentState.data))
        },
        onDiscardProgress = {
          state.eventSink(LibraryItemUiEvent.DiscardProgress(contentState.data))
        },
        onTimeInBookChange = { enabled ->
          state.eventSink(LibraryItemUiEvent.TimeInBookChange(contentState.data, enabled))
        },
      )
    }
  }

  if (showAddToCollectionDialog) {
    addToCollectionDialog.Content(
      item = state.libraryItemContentState.dataOrNull!!,
      onDismiss = { showAddToCollectionDialog = false },
      modifier = Modifier,
    )
  }

  var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }
  val postNotificationPermissionState = rememberPostNotificationPermissionState {
    if (it) {
      state.eventSink(LibraryItemUiEvent.DownloadClick(doNotShowDownloadConfirmationAgain))
      showConfirmDownloadDialog = false
    }
  }

  if (showConfirmDownloadDialog) {
    ConfirmDownloadDialog(
      item = state.libraryItemContentState.dataOrNull!!,
      onConfirm = { doNotShowAgain ->
        if (postNotificationPermissionState is PermissionState.Granted) {
          state.eventSink(LibraryItemUiEvent.DownloadClick(doNotShowAgain))
          showConfirmDownloadDialog = false
        } else {
          doNotShowDownloadConfirmationAgain = doNotShowAgain
          postNotificationPermissionState.launchPermissionRequest()
        }
      },
      onDismissRequest = { showConfirmDownloadDialog = false },
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LoadedState(
  item: LibraryItem,
  sharedTransitionKey: String,
  seriesContentState: LoadState<out List<LibraryItem>>,
  mediaProgressState: LoadState<out MediaProgress?>,
  showTimeInBook: Boolean,
  offlineDownload: OfflineDownload?,
  onAuthorClick: () -> Unit,
  onNarratorClick: () -> Unit,
  onChapterClick: (Chapter) -> Unit,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onRemoveDownloadClick: () -> Unit,
  onStopDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
  onSeriesClick: () -> Unit,
  onTimeInBookChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  scrollState: ScrollState = rememberScrollState(),
) = SharedElementTransitionScope {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(contentPadding),
  ) {
    CoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
      placeholder = painterResource(Res.drawable.placeholder_book),
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          vertical = 16.dp,
        ),
      sharedElementModifier = Modifier
        .sharedElement(
          sharedContentState = rememberSharedContentState(
            LibraryItemSharedTransitionKey(
              id = sharedTransitionKey,
              type = LibraryItemSharedTransitionKey.ElementType.Image,
            ),
          ),
          animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
        ),
    )

    Spacer(Modifier.height(16.dp))

    Text(
      text = item.media.metadata.title ?: stringResource(Res.string.unknown_title),
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.SemiBold,
      fontStyle = if (item.media.metadata.title == null) FontStyle.Italic else null,
      fontFamily = PaytoneOneFontFamily,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    )

    item.media.metadata.subtitle?.let { subtitle ->
      Spacer(Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      )
    }

    Spacer(Modifier.height(4.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .alpha(0.65f),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Outlined.Schedule,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = item.media.durationInMillis.milliseconds.readoutFormat(),
        style = MaterialTheme.typography.titleSmall,
      )
      Spacer(Modifier.width(26.dp))
    }

    Spacer(Modifier.height(24.dp))

    AuthorNarratorBar(
      metadata = item.media.metadata,
      onAuthorClick = onAuthorClick,
      onNarratorClick = onNarratorClick,
    )

    Spacer(Modifier.height(24.dp))

    mediaProgressState.onLoaded { mediaProgress ->
      if (mediaProgress != null && mediaProgress.progress > 0f) {
        Spacer(Modifier.height(16.dp))
        MediaProgressBar(
          progress = mediaProgress,
          modifier = Modifier
            .padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(16.dp))
      }
    }

    ControlBar(
      offlineDownload = offlineDownload,
      mediaProgress = mediaProgressState.dataOrNull,
      isCurrentListening = false,
      onPlayClick = onPlayClick,
      onDownloadClick = onDownloadClick,
      onMarkFinished = onMarkFinished,
      onMarkNotFinished = onMarkNotFinished,
      onDiscardProgress = onDiscardProgress,
      modifier = Modifier
        .padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(16.dp))

    if (offlineDownload != null && offlineDownload.state != OfflineDownload.State.None) {
      OfflineStatusCard(
        offlineDownload = offlineDownload,
        onDeleteClick = onRemoveDownloadClick,
        onStopClick = onStopDownloadClick,
        modifier = Modifier
          .padding(
            horizontal = 16.dp,
          ),
      )

      Spacer(Modifier.height(16.dp))
    }

    if (item.media.metadata.publisher != null && item.media.metadata.publishedYear != null) {
      Text(
        text = buildAnnotatedString {
          append("Published by ")
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(item.media.metadata.publisher)
          }
          append(" in ")
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(item.media.metadata.publishedYear)
          }
        },
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = 16.dp,
          ),
      )

      Spacer(Modifier.height(16.dp))
    }

    GenreChips(item.media.metadata.genres)
    Spacer(Modifier.height(16.dp))

    seriesContentState.onLoaded { seriesBooks ->
      MetadataHeader(
        title = "Series",
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      SeriesMetadata(
        seriesName = item.media.metadata.seriesSequence?.name
          ?: item.media.metadata.seriesName
          ?: "--",
        seriesBooks = seriesBooks,
        modifier = Modifier
          .clickable(onClick = onSeriesClick)
          .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
          ),
      )
    }

    item.media.metadata.description?.let { desc ->
      Spacer(Modifier.height(16.dp))
      MetadataHeader(
        title = "Summary",
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      Spacer(Modifier.height(8.dp))
      ItemDescription(
        description = desc,
      )
    }

    Spacer(Modifier.height(24.dp))

    if (item.media.chapters.isNotEmpty()) {
      HorizontalDivider(Modifier.fillMaxWidth())
      MetadataHeader(
        title = stringResource(Res.string.header_chapters),
        modifier = Modifier
          .height(56.dp)
          .padding(
            horizontal = 16.dp,
          ),
        trailingContent = {
          Switch(
            checked = showTimeInBook,
            onCheckedChange = onTimeInBookChange,
            thumbContent = {
              Icon(
                if (showTimeInBook) CampfireIcons.Rounded.BookRibbon else Icons.Rounded.Timer,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
              )
            },
          )
        },
      )

      item.media.chapters.forEach { chapter ->
        val progress = mediaProgressState.dataOrNull?.let { mediaProgress ->
          (mediaProgress.currentTime.seconds - chapter.start.seconds) / chapter.duration
        }?.toFloat() ?: 0f

        DurationListItem(
          title = chapter.title,
          duration = if (showTimeInBook) {
            chapter.start.seconds
          } else {
            chapter.duration
          },
          progress = progress,
          modifier = Modifier
            .clickable {
              onChapterClick(chapter)
            },
        )
      }
    }
  }
}
