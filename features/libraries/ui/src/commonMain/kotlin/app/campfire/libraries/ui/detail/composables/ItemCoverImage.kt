package app.campfire.libraries.ui.detail.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.error_item_image_message
import campfire.features.libraries.ui.generated.resources.placeholder_book
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CoverImageSize = 256.dp
private val CoverImageCornerRadius = 32.dp
private val CoverImageVerticalSpacing = 16.dp

@Composable
internal fun ItemCoverImage(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        vertical = CoverImageVerticalSpacing,
      ),
    contentAlignment = Alignment.Center,
  ) {
    val painter = rememberAsyncImagePainter(imageUrl)

    Image(
      painter = painter,
      contentDescription = contentDescription,
      modifier = Modifier
        .size(CoverImageSize)
        .clip(RoundedCornerShape(CoverImageCornerRadius))
    )

    val painterState by painter.state.collectAsState()
    when (painterState) {
      is AsyncImagePainter.State.Error -> ErrorCover()
      is AsyncImagePainter.State.Loading -> LoadingCover()
      AsyncImagePainter.State.Empty -> PlaceholderCover()
      else -> Unit
    }
  }
}

@Composable
private fun PlaceholderCover(
  modifier: Modifier = Modifier,
) {
  Image(
    painterResource(Res.drawable.placeholder_book),
    contentDescription = null,
    modifier = modifier
      .size(CoverImageSize)
      .clip(RoundedCornerShape(CoverImageCornerRadius)),
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

@Composable
private fun ErrorCover(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(CoverImageCornerRadius),
      )
      .size(CoverImageSize),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(Icons.Rounded.Error, contentDescription = null)
    Text(
      text = stringResource(Res.string.error_item_image_message),
      style = MaterialTheme.typography.labelMedium,
    )
  }
}
