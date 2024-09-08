package app.campfire.libraries.ui.detail

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.util.Palette
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.screens.LibraryItemScreen
import app.campfire.core.di.UserScope
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.composables.ControlBar
import app.campfire.libraries.ui.detail.composables.DurationListItem
import app.campfire.libraries.ui.detail.composables.ItemCoverImage
import app.campfire.libraries.ui.detail.composables.ItemDescription
import app.campfire.libraries.ui.detail.composables.ItemMetadata
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.composables.MetadataHeader
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.error_library_item_message
import campfire.features.libraries.ui.generated.resources.header_chapters
import campfire.features.libraries.ui.generated.resources.unknown_title
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.stringResource

@CircuitInject(LibraryItemScreen::class, UserScope::class)
@Composable
fun LibraryItem(
  state: LibraryItemUiState,
  modifier: Modifier = Modifier,
) {
  var palette by remember { mutableStateOf<Palette?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
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
    modifier = modifier,
  ) { paddingValues ->
    when (val contentState = state.libraryItemContentState) {
      LibraryItemContentState.Error -> ErrorListState(
        message = stringResource(Res.string.error_library_item_message),
        modifier = Modifier.padding(paddingValues),
      )

      LibraryItemContentState.Loading -> LoadingListState(Modifier.padding(paddingValues))
      is LibraryItemContentState.Loaded -> LoadedState(
        item = contentState.item,
        modifier = modifier,
        contentPadding = paddingValues,
        onChapterClick = { chapter ->
        },
        onColorPalette = { p ->
          palette = p
        },
      )
    }
  }
}

@Composable
fun LoadedState(
  item: LibraryItem,
  onChapterClick: (Chapter) -> Unit,
  onColorPalette: (Palette) -> Unit,
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
    ItemCoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
    )

    Spacer(Modifier.height(16.dp))

    Text(
      text = item.media.metadata.title ?: stringResource(Res.string.unknown_title),
      style = MaterialTheme.typography.headlineMedium,
      fontStyle = if (item.media.metadata.title == null) FontStyle.Italic else null,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(16.dp))

    item.userMediaProgress?.let { progress ->
      MediaProgressBar(
        progress = progress,
        modifier = Modifier
          .padding(horizontal = 16.dp),
      )
      Spacer(Modifier.height(16.dp))
    }

    ControlBar(
      hasProgress = item.userMediaProgress != null,
      isCurrentListening = false,
      onPlayClick = {},
      onDownloadClick = {},
      onMarkFinished = {},
      onDiscardProgress = {},
      onAddToPlaylist = {},
      onAddToCollection = {},
      modifier = Modifier
        .padding(horizontal = 16.dp),
    )

    ItemMetadata(
      item = item,
      modifier = Modifier
        .padding(16.dp),
    )

    item.media.metadata.description?.let { desc ->
      Spacer(Modifier.height(16.dp))
      ItemDescription(
        description = desc,
      )
    }

    Spacer(Modifier.height(16.dp))

    if (item.media.chapters.isNotEmpty()) {
      HorizontalDivider(Modifier.fillMaxWidth())
      MetadataHeader(
        title = stringResource(Res.string.header_chapters),
      )

      item.media.chapters.forEach { chapter ->
        DurationListItem(
          title = chapter.title,
          duration = chapter.start.times(100f)
            .roundToLong()
            .milliseconds,
          modifier = Modifier
            .clickable {
              onChapterClick(chapter)
            }
            .padding(horizontal = 16.dp),
        )
      }
    }
  }
}
