package app.campfire.sessions.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

@Composable
internal fun Thumbnail(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  val painter = rememberAsyncImagePainter(imageUrl)
  val shape = RoundedCornerShape(8.dp)
  Image(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier
      .size(ThumbnailSize)
      .clip(shape)
      .border(1.dp, MaterialTheme.colorScheme.secondary, shape),
  )
}

private val ThumbnailSize = 56.dp
