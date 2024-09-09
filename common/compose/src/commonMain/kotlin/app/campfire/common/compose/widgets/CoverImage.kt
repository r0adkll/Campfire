package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Author
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_man
import campfire.common.compose.generated.resources.placeholder_woman
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource

private val CoverImageSize = 256.dp
private val CoverImageCornerRadius = 32.dp
private val CoverImageVerticalSpacing = 16.dp

@Composable
fun CoverImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  placeholder: Painter? = null,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        vertical = CoverImageVerticalSpacing,
      ),
    contentAlignment = Alignment.Center,
  ) {
    val painter = rememberAsyncImagePainter(
      model = imageUrl,
      error = placeholder,
    )

    Image(
      painter = painter,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(CoverImageSize)
        .clip(RoundedCornerShape(CoverImageCornerRadius)),
    )

    val painterState by painter.state.collectAsState()
    when (painterState) {
      is AsyncImagePainter.State.Loading -> LoadingCover()
      else -> Unit
    }
  }
}

@Composable
fun AuthorCoverImage(
  author: Author,
  modifier: Modifier = Modifier,
) {
  val placeHolderResource = remember {
    if (Random.nextBoolean()) {
      Res.drawable.placeholder_man
    } else {
      Res.drawable.placeholder_woman
    }
  }
  CoverImage(
    imageUrl = author.imagePath ?: "",
    contentDescription = author.name,
    placeholder = painterResource(placeHolderResource),
    modifier = modifier,
  )
}

@Composable
private fun LoadingCover(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(CoverImageCornerRadius),
      )
      .size(CoverImageSize),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}
