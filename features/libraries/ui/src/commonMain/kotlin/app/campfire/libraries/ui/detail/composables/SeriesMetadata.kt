package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.campfire.core.model.LibraryItem
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

@Composable
internal fun SeriesMetadata(
  seriesName: String,
  seriesBooks: List<LibraryItem>,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SeriesIcon(
      books = seriesBooks,
    )
    Spacer(Modifier.width(16.dp))
    Text(
      text = seriesName,
      style = MaterialTheme.typography.titleSmall,
    )
  }
}

@Composable
private fun SeriesIcon(
  books: List<LibraryItem>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .size(SeriesIconSize)
      .clip(RoundedCornerShape(SeriesIconCornerRadius)),
  ) {
    Row(
      modifier = Modifier.weight(1f),
    ) {
      SeriesBookImage(
        book = books.getOrNull(0),
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(SeriesImageSpacing))
      SeriesBookImage(
        book = books.getOrNull(1),
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(Modifier.height(SeriesImageSpacing))
    Row(
      modifier = Modifier.weight(1f),
    ) {
      SeriesBookImage(
        book = books.getOrNull(2),
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(SeriesImageSpacing))
      SeriesBookImage(
        book = books.getOrNull(3),
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun SeriesBookImage(
  book: LibraryItem?,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        MaterialTheme.colorScheme.primaryContainer,
      ),
  ) {
    val imageUrl = book?.media?.coverImageUrl
    if (imageUrl != null) {
      CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        BookImage(
          imageUrl = imageUrl,
          contentDescription = book.media.metadata.title,
        )
      }
    }
  }
}

@Composable
private fun BookImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val painter = rememberAsyncImagePainter(
      model = imageUrl,
    )

    Image(
      painter = painter,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    val painterState by painter.state.collectAsState()
    when (painterState) {
      is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
      else -> Unit
    }
  }
}

private val SeriesIconSize = 64.dp
private val SeriesIconCornerRadius = 8.dp
private val SeriesImageSpacing = 2.dp
