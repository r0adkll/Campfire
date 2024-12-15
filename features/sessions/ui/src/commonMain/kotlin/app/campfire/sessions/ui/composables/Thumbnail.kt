package app.campfire.sessions.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

@Composable
internal fun Thumbnail(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: Dp = ThumbnailSize,
  cornerRadius: Dp = CornerRadius,
  borderWidth: Dp = BorderWidth,
  borderColor: Color = MaterialTheme.colorScheme.secondary,
) {
  val painter = rememberAsyncImagePainter(imageUrl)
  val shape = RoundedCornerShape(cornerRadius)
  Image(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier
      .size(size)
      .clip(shape)
      .border(borderWidth, borderColor, shape),
  )
}

private val ThumbnailSize = 56.dp
private val CornerRadius = 8.dp
private val BorderWidth = 1.dp
