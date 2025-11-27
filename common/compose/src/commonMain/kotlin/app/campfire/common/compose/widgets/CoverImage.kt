package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Author
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_person
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.r0adkll.material3.themebuilder.coil.collectAsSwatch
import com.r0adkll.material3.themebuilder.coil.observeAsImageBitmap
import com.r0adkll.swatchbuckler.compose.Swatch
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import org.jetbrains.compose.resources.painterResource

val CoverImageSize = 256.dp
val CoverImageCornerRadius = 32.dp
val CoverImageShape = RoundedCornerShape(CoverImageCornerRadius)

/**
 * Quantizing image pixels is a VERY intensive process and its best to perform this
 * using dedicated threads and not the default dispatcher
 */
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
private val quantizerDispatcher = newFixedThreadPoolContext(2, "QuantizerThreads")

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CoverImage(
  imageUrl: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  sharedElementModifier: Modifier = Modifier,
  placeholder: Painter? = null,
  size: Dp = CoverImageSize,
  shape: Shape = CoverImageShape,
  swatchListener: (suspend (Swatch) -> Unit)? = null,
  imageBitmapListener: ((ImageBitmap) -> Unit)? = null,
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

    if (swatchListener != null) {
      val palette by painter.collectAsSwatch(
        dispatcher = quantizerDispatcher,
      )
      LaunchedEffect(palette) {
        palette?.let {
          swatchListener(it)
        }
      }
    }

    if (imageBitmapListener != null) {
      LaunchedEffect(Unit) {
        painter.state
          .observeAsImageBitmap()
          .collect {
            imageBitmapListener(it)
          }
      }
    }

    Image(
      painter = painter,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = sharedElementModifier
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AuthorCoverImage(
  author: Author,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val placeHolderResource = remember {
    Res.drawable.placeholder_person
  }
  CoverImage(
    imageUrl = author.imagePath ?: "",
    contentDescription = author.name,
    placeholder = painterResource(placeHolderResource),
    modifier = modifier,
    sharedElementModifier = Modifier
      .sharedElement(
        sharedContentState = rememberSharedContentState(
          AuthorSharedTransitionKey(
            id = author.id,
            type = AuthorSharedTransitionKey.ElementType.Image,
          ),
        ),
        animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
      ),
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
