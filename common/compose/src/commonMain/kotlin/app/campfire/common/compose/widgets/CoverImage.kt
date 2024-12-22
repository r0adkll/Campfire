package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Author
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_man
import campfire.common.compose.generated.resources.placeholder_woman
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource

val CoverImageSize = 256.dp
private val CoverImageCornerRadius = 32.dp

@Composable
fun CoverImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  placeholder: Painter? = null,
  size: Dp = CoverImageSize,
  shape: Shape = RoundedCornerShape(CoverImageCornerRadius),
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val painter = key(imageUrl) {
      rememberAsyncImagePainter(
        model = imageUrl,
        error = placeholder,
      )
    }

    Image(
      painter = painter,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(size)
        .clip(shape),
    )

    val painterState by painter.state.collectAsState()
    when (painterState) {
      is AsyncImagePainter.State.Loading -> LoadingCover(
        shape = shape,
        size = size,
      )
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
  shape: Shape,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = shape,
      )
      .size(size),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}
