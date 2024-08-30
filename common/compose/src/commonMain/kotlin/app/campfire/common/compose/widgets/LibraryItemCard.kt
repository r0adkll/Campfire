package app.campfire.common.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.campfire.core.model.LibraryItem
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryItemCard(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
  ) {
    Box {
      ItemThumbnail(
        imageUrl = item.media.coverImageUrl,
        contentDescription = item.media.metadata.title,
        cornerRadius = ThumbnailCornerSize,
        modifier = Modifier
          .aspectRatio(1f)
          .fillMaxWidth()
          .widthIn(max = CardMaxWidth),
      )
      // TODO: Progress Bar
    }
    Column(
      Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = item.media.metadata.title ?: stringResource(Res.string.unknown_library_title),
        style = MaterialTheme.typography.titleSmall,
        fontStyle = if (item.media.metadata.title == null) FontStyle.Italic else null,
        maxLines = 1,
        modifier = Modifier.basicMarquee(),
      )
      Text(
        text = item.media.metadata.authorName ?: stringResource(Res.string.unknown_author_name),
        style = MaterialTheme.typography.bodySmall,
        fontStyle = if (item.media.metadata.authorName == null) FontStyle.Italic else null,
        maxLines = 1,
        modifier = Modifier.basicMarquee(),
      )
    }
  }
}
