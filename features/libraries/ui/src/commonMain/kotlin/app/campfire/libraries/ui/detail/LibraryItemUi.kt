package app.campfire.libraries.ui.detail

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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.coroutines.onLoaded
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.composables.AuthorNarratorBar
import app.campfire.libraries.ui.detail.composables.ControlBar
import app.campfire.libraries.ui.detail.composables.DurationListItem
import app.campfire.libraries.ui.detail.composables.GenreChips
import app.campfire.libraries.ui.detail.composables.ItemDescription
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.composables.SeriesMetadata
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.error_library_item_message
import campfire.features.libraries.ui.generated.resources.header_chapters
import campfire.features.libraries.ui.generated.resources.placeholder_book
import campfire.features.libraries.ui.generated.resources.unknown_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  state: LibraryItemUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
          IconButton(
            onClick = {},
          ) {
            Icon(Icons.Rounded.QueuePlayNext, contentDescription = null)
          }
          IconButton(
            onClick = {},
          ) {
            Icon(Icons.Rounded.Cast, contentDescription = null)
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
        seriesContentState = state.seriesContentState,
        mediaProgressState = state.mediaProgressState,
        modifier = modifier,
        contentPadding = paddingValues,
        onChapterClick = { chapter ->
          state.eventSink(LibraryItemUiEvent.ChapterClick(contentState.data, chapter))
        },
        onPlayClick = {
          state.eventSink(LibraryItemUiEvent.PlayClick(contentState.data))
        },
        onDownloadClick = {
        },
        onSeriesClick = {
          state.eventSink(LibraryItemUiEvent.SeriesClick(contentState.data))
        },
        onAddToPlaylist = {
        },
        onAddToCollection = {
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
      )
    }
  }
}

@Composable
fun LoadedState(
  item: LibraryItem,
  seriesContentState: LoadState<out List<LibraryItem>>,
  mediaProgressState: LoadState<out MediaProgress?>,
  onChapterClick: (Chapter) -> Unit,
  onPlayClick: () -> Unit,
  onDownloadClick: () -> Unit,
  onMarkFinished: () -> Unit,
  onMarkNotFinished: () -> Unit,
  onDiscardProgress: () -> Unit,
  onAddToPlaylist: () -> Unit,
  onAddToCollection: () -> Unit,
  onSeriesClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
  scrollState: ScrollState = rememberScrollState(),
) {
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
      mediaProgress = mediaProgressState.dataOrNull,
      isCurrentListening = false,
      onPlayClick = onPlayClick,
      onDownloadClick = onDownloadClick,
      onMarkFinished = onMarkFinished,
      onMarkNotFinished = onMarkNotFinished,
      onDiscardProgress = onDiscardProgress,
      onAddToPlaylist = onAddToPlaylist,
      onAddToCollection = onAddToCollection,
      modifier = Modifier
        .padding(horizontal = 16.dp),
    )

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
            vertical = 16.dp,
          ),
      )
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
        modifier = Modifier.padding(
          horizontal = 16.dp,
          vertical = 16.dp,
        ),
      )

      item.media.chapters.forEach { chapter ->
        val progress = mediaProgressState.dataOrNull?.let { mediaProgress ->
          (mediaProgress.currentTime.seconds - chapter.start.seconds) / chapter.duration
        }?.toFloat() ?: 0f

        DurationListItem(
          title = chapter.title,
          duration = chapter.start.seconds,
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
