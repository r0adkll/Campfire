package app.campfire.common.compose.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.cardElevation
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

@Composable
fun LibraryItemCard(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  val contentLayout = LocalContentLayout.current

  ElevatedCard(
    modifier = modifier,
    elevation = contentLayout.cardElevation,
  ) {
    val shape = RoundedCornerShape(ThumbnailCornerSize)
    Box(
      modifier = Modifier.clip(shape),
    ) {
      ItemImage(
        imageUrl = item.media.coverImageUrl,
        contentDescription = item.media.metadata.title,
        modifier = Modifier
          .aspectRatio(1f)
          .fillMaxWidth()
          .widthIn(max = CardMaxWidth)
          .clip(shape),
      )

      item.userMediaProgress?.let { mediaProgress ->
        MediaProgressBar(
          mediaProgress = mediaProgress,
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        )
      }
    }
    Column(
      Modifier.padding(
        vertical = 16.dp,
      ),
    ) {
      Text(
        text = item.media.metadata.title ?: stringResource(Res.string.unknown_library_title),
        style = MaterialTheme.typography.titleSmall,
        fontStyle = if (item.media.metadata.title == null) FontStyle.Italic else null,
        maxLines = 1,
        modifier = Modifier
          .basicMarquee(
            velocity = LibraryItemMarqueeVelocity,
          )
          .padding(horizontal = 16.dp),
      )
      Text(
        text = item.media.metadata.authorName
          ?: item.media.metadata.authors.firstOrNull()?.name
          ?: stringResource(Res.string.unknown_author_name),
        style = MaterialTheme.typography.bodySmall,
        fontStyle = if (item.media.metadata.authorName == null) FontStyle.Italic else null,
        maxLines = 1,
        modifier = Modifier
          .basicMarquee(
            velocity = LibraryItemMarqueeVelocity,
          )
          .padding(horizontal = 16.dp),
      )
    }
  }
}

@Composable
private fun MediaProgressBar(
  mediaProgress: MediaProgress,
  modifier: Modifier = Modifier,
  trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
  progressColor: Color = MaterialTheme.colorScheme.primary,
) {
  Canvas(
    modifier = modifier
      .height(ProgressBarHeight),
  ) {
    // Draw Track
    drawRect(
      color = trackColor,
      size = size,
      alpha = ProgressBarAlpha,
    )

    val cornerRadiusPx = ProgressBarHeight.toPx() / 2f
    val progressSize = size.copy(
      width = (size.width * mediaProgress.actualProgress) + cornerRadiusPx,
    )
    drawRoundRect(
      color = progressColor,
      topLeft = Offset(x = -cornerRadiusPx, y = 0f),
      size = progressSize,
      cornerRadius = CornerRadius(cornerRadiusPx),
    )
  }
}

private val LibraryItemMarqueeVelocity = 40.dp

private val ProgressBarHeight = 12.dp
private const val ProgressBarAlpha = 0.5f
