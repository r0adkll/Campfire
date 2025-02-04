package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

@Composable
fun ItemImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
) {
  Box(
    modifier = modifier,
  ) {
    val painter = rememberAsyncImagePainter(imageUrl)

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
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = Modifier
        .fillMaxSize(),
    )
  }
}

@Composable
internal fun ImageError(
  errorMessage: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(MaterialTheme.colorScheme.errorContainer)
      .fillMaxSize(),
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
internal fun ImageLoading(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
