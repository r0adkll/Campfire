package app.campfire.common.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.campfire.core.logging.bark
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_man
import campfire.common.compose.generated.resources.placeholder_woman
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthorCard(
  author: Author,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
  ) {
    Box(
      modifier = modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .widthIn(max = CardMaxWidth)
        .clip(RoundedCornerShape(ThumbnailCornerSize)),
    ) {
      val painter = rememberAsyncImagePainter(
        model = author.imagePath,
        onError = {
          bark(throwable = it.result.throwable) { "Author image loading error: ${it.result.request.data}" }
        }
      )

      val imageState by painter.state.collectAsState()
      when (imageState) {
        is AsyncImagePainter.State.Error -> PlaceholderImage()
        is AsyncImagePainter.State.Loading -> ImageLoading()
        // Do nothing in the other states
        else -> Unit
      }

      Image(
        painter,
        contentDescription = author.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxSize(),
      )
    }
    Column(
      Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = author.name,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        modifier = Modifier.basicMarquee(),
      )
      author.description?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
        )
      }
    }
  }
}

@Composable
private fun PlaceholderImage(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize(),
  ) {
    val placeHolderResource = remember {
      if (Random.nextBoolean()) {
        Res.drawable.placeholder_man
      } else {
        Res.drawable.placeholder_woman
      }
    }
    Image(
      painterResource(placeHolderResource),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
  }
}
