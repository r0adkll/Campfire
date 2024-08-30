package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.core.model.LibraryItem
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.added_time_ago
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource

private val ThumbnailSize = 56.dp
private val ThumbnailCornerSize = 8.dp

@Composable
fun LibraryListItem(
  item: LibraryItem,
  modifier: Modifier = Modifier
) {
  ListItem(
    leadingContent = { Thumbnail(item) },
    overlineContent = {
      Text(item.media.metadata.authorName ?: stringResource(Res.string.unknown_author_name))
    },
    headlineContent = {
      Text(item.media.metadata.title ?: stringResource(Res.string.unknown_library_title))
    },
    supportingContent = {
      Text(stringResource(Res.string.added_time_ago, item.addedAtMillis.timeAgo))
    },
    modifier = modifier,
  )
}

@Composable
private fun Thumbnail(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(ThumbnailSize)
  ) {
    val painter = rememberAsyncImagePainter(item.media.coverImageUrl)

    val imageState by painter.state.collectAsState()
    when (val state = imageState) {
      is AsyncImagePainter.State.Error -> ImageError(
        errorMessage = "${state.result.throwable.message}",
      )
      is AsyncImagePainter.State.Loading -> ImageLoading()
      // Do nothing in the other states
      else -> Unit
    }

    Image(
      painter,
      contentDescription = item.media.metadata.title,
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(ThumbnailCornerSize)),
    )
  }
}

@Composable
private fun ImageError(
  errorMessage: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(MaterialTheme.colorScheme.errorContainer)
      .fillMaxSize()
      .clip(RoundedCornerShape(ThumbnailCornerSize)),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CompositionLocalProvider(
      LocalContentColor provides MaterialTheme.colorScheme.onErrorContainer,
    ) {
      Icon(
        Icons.Rounded.ErrorOutline,
        contentDescription = null,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

@Composable
private fun ImageLoading(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .fillMaxSize()
      .clip(RoundedCornerShape(ThumbnailCornerSize)),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
