package app.campfire.common.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Series

private val BookImageSize = 180.dp
private val BookCornerSize = 12.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCard(
  series: Series,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
  ) {
    // TODO: We probably need a custom layout to overlap these items in a row
    LazyRow {
      series.books?.let { books ->
        items(books) { book ->
          ItemThumbnail(
            imageUrl = book.media.coverImageUrl,
            contentDescription = book.media.metadata.title,
            cornerRadius = BookCornerSize,
            modifier = Modifier
              .size(BookImageSize),
          )
        }
      }
    }
    Column(
      Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = series.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        modifier = Modifier.basicMarquee(),
      )
      series.description?.let { desc ->
        Text(
          text = desc,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 2,
          modifier = Modifier.basicMarquee(),
        )
      }
    }
  }
}
