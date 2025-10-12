@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.cardElevation
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.offline.OfflineStatus
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_book
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp
private val ThumbnailCornerSize = 12.dp

data class LibraryItemSharedTransitionKey(
  val id: String,
  val type: ElementType,
) {
  enum class ElementType {
    Image,
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryItemCard(
  item: LibraryItem,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String = item.id,
  sharedTransitionZIndex: Float = 0f,
  isSelectable: Boolean = false,
  selected: Boolean = false,
  offlineStatus: OfflineStatus = OfflineStatus.None,
) {
  val contentLayout = LocalContentLayout.current

  ElevatedCard(
    modifier = modifier,
    elevation = contentLayout.cardElevation,
  ) {
    Box {
      Column {
        LibraryItemCardImage(
          item = item,
          sharedTransitionKey = sharedTransitionKey,
          sharedTransitionZIndex = sharedTransitionZIndex,
          offlineStatus = offlineStatus,
        )
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
  sharedTransitionKey: String,
  sharedTransitionZIndex: Float,
  offlineStatus: OfflineStatus,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val shape = RoundedCornerShape(ThumbnailCornerSize)
  Box(
    modifier = modifier
      .clip(shape),
  ) {
    CoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
      placeholder = painterResource(Res.drawable.placeholder_book),
      shape = shape,
      modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .widthIn(max = CardMaxWidth)
        .clip(shape),
      sharedElementModifier = Modifier
        .fluentIf<Modifier>(findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation) != null) {
          sharedElement(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Image,
              ),
            ),
            animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
            zIndexInOverlay = sharedTransitionZIndex,
          )
        },
    )

    item.userMediaProgress?.let { mediaProgress ->
      MediaProgressBar(
        mediaProgress = mediaProgress,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(),
      )
    }

    if (offlineStatus != OfflineStatus.None) {
      OfflineStatusIndicator(
        status = offlineStatus,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(
            end = 8.dp,
            top = 8.dp,
          ),
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

@Composable
fun OfflineStatusIndicator(
  status: OfflineStatus,
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
  tint: Color = Color.White,
) {
  when (status) {
    OfflineStatus.None -> Unit
    is OfflineStatus.Downloading -> {
      CircularProgressIndicator(
        progress = { status.progress },
        strokeWidth = 3.dp,
        color = tint,
        modifier = modifier
          .size(size),
      )
    }

    OfflineStatus.Queued -> {
      CircularProgressIndicator(
        strokeWidth = 3.dp,
        color = tint,
        modifier = modifier
          .size(size),
      )
    }

    OfflineStatus.Available -> {
      Icon(
        Icons.Rounded.CloudDone,
        contentDescription = null,
        tint = tint,
        modifier = modifier
          .size(size),
      )
    }

    OfflineStatus.Failed -> {
      Icon(
        Icons.Rounded.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier
          .size(size),
      )
    }
  }
}

private val LibraryItemMarqueeVelocity = 40.dp

private val ProgressBarHeight = 12.dp
private const val ProgressBarAlpha = 0.5f
