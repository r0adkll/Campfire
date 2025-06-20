package app.campfire.common.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.cardElevation
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_book
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

@Composable
fun LibraryItemCard(
  item: LibraryItem,
  modifier: Modifier = Modifier,
  isSelectable: Boolean = false,
  selected: Boolean = false,
) {
  val contentLayout = LocalContentLayout.current

  ElevatedCard(
    modifier = modifier,
    elevation = contentLayout.cardElevation,
  ) {
    Box {
      Column {
        LibraryItemCardImage(item)
        LibraryItemCardInformation(item)
      }

      LibraryItemCardEditingScrim(
        isSelectable = isSelectable,
        selected = selected,
        modifier = Modifier.matchParentSize(),
      )
    }
  }
}

@Composable
private fun LibraryItemCardImage(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(ThumbnailCornerSize)
  Box(
    modifier = modifier
      .clip(shape),
  ) {
    CoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
      placeholder = painterResource(Res.drawable.placeholder_book),
      shape = RectangleShape,
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
}

@Composable
private fun LibraryItemCardInformation(
  item: LibraryItem,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier.padding(
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

@Composable
private fun LibraryItemCardEditingScrim(
  isSelectable: Boolean,
  selected: Boolean,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = isSelectable,
    modifier = modifier,
  ) {
    val selectedBorderAlpha by animateFloatAsState(if (selected) 1f else 0.75f)
    val selectedBorderSize by animateDpAsState(if (selected) 2.dp else 1.dp)
    val selectedBackgroundAlpha by animateFloatAsState(if (selected) 0.75f else 0.3f)

    Box(
      modifier = Modifier
        .clip(CardDefaults.elevatedShape)
        .fillMaxSize()
        .background(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = selectedBackgroundAlpha,
          ),
          shape = CardDefaults.elevatedShape,
        )
        .border(
          width = selectedBorderSize,
          color = MaterialTheme.colorScheme.secondary.copy(
            alpha = selectedBorderAlpha,
          ),
          shape = CardDefaults.elevatedShape,
        ),
    ) {
      Icon(
        if (selected) Icons.Rounded.CheckCircle else Icons.Outlined.Circle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(16.dp),
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
