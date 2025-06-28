package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.core.model.LibraryItem
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.added_time_ago
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import org.jetbrains.compose.resources.stringResource

private val ThumbnailSize = 56.dp
private val ThumbnailCornerSize = 8.dp

@Composable
fun LibraryListItem(
  item: LibraryItem,
  modifier: Modifier = Modifier,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  ListItem(
    leadingContent = {
      ItemImage(
        imageUrl = item.media.coverImageUrl,
        contentDescription = item.media.metadata.title,
        modifier = Modifier
          .size(ThumbnailSize)
          .clip(RoundedCornerShape(ThumbnailCornerSize)),
      )
    },
    overlineContent = {
      Text(item.media.metadata.authorName ?: stringResource(Res.string.unknown_author_name))
    },
    headlineContent = {
      Text(item.media.metadata.title ?: stringResource(Res.string.unknown_library_title))
    },
    supportingContent = {
      Text(stringResource(Res.string.added_time_ago, item.addedAtMillis.timeAgo))
    },
    trailingContent = trailingContent,
    modifier = modifier,
  )
}
