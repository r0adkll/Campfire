@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.compose.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.common.compose.extensions.thenIf
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.FatCheck
import app.campfire.common.compose.theme.colorScheme
import app.campfire.common.compose.util.rememberThemeDispatcherListener
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.offline.OfflineStatus
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.placeholder_book
import campfire.common.compose.generated.resources.unknown_author_name
import campfire.common.compose.generated.resources.unknown_library_title
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardMaxWidth = 400.dp

data class LibraryItemSharedTransitionKey(
  val id: String,
  val type: ElementType,
) {
  enum class ElementType {
    Image,
    Bounds,
    Title,
  }
}

val LocalItemCardMarquee = compositionLocalOf { true }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryItemCard(
  item: LibraryItem,
  episode: PodcastEpisode? = null,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String = item.id,
  sharedTransitionZIndex: Float = 0f,
  isSelectable: Boolean = false,
  selected: Boolean = false,
  showInformation: Boolean = true,
  marqueeEnabled: Boolean = LocalItemCardMarquee.current,
  offlineStatus: OfflineStatus = OfflineStatus.None,
  progress: MediaProgress? = item.userMediaProgress,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
  colors: CardColors = CardDefaults.elevatedCardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
  ),
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  ElevatedContentCard(
    modifier = modifier
      .thenIfNotNull(animationScope) { scope ->
        sharedBounds(
          sharedContentState = rememberSharedContentState(
            LibraryItemSharedTransitionKey(
              id = sharedTransitionKey,
              type = LibraryItemSharedTransitionKey.ElementType.Bounds,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
    onClick = onClick,
    colors = colors,
    shape = shape,
  ) {
    Box {
      Column {
        LibraryItemCardImage(
          item = item,
          sharedTransitionKey = sharedTransitionKey,
          sharedTransitionZIndex = sharedTransitionZIndex,
          shape = shape,
          decorator = { isTransitioning ->
            // Render the progress decoration
            ProgressDecorator(
              progress = progress,
              isTransitioning = isTransitioning,
              large = showInformation,
            )

            if (!episode?.episode.isNullOrBlank()) {
              // Render the podcast episode decoration
              PodcastEpisodeDecorator(
                episode = episode,
                offlineStatus = offlineStatus,
                isTransitioning = isTransitioning,
              )
            } else if (item.isEbookOnly) {
              // Render the ebook format decoration
              EbookFormatDecorator(
                format = item.media.ebookFormat.orEmpty(),
                isTransitioning = isTransitioning,
              )
            } else {
              // Render the offline status decoration
              OfflineStatusDecorator(
                offlineStatus = offlineStatus,
                isTransitioning = isTransitioning,
              )
            }
          },
        )
        if (showInformation) {
          if (episode != null) {
            LibraryItemCardInformation(
              item = item,
              episode = episode,
              marqueeEnabled = marqueeEnabled,
              sharedTransitionKey = sharedTransitionKey,
            )
          } else {
            LibraryItemCardInformation(
              item = item,
              marqueeEnabled = marqueeEnabled,
              sharedTransitionKey = sharedTransitionKey,
            )
          }
        }
      }

      LibraryItemCardEditingScrim(
        isSelectable = isSelectable,
        selected = selected,
        modifier = Modifier.matchParentSize(),
        shape = shape,
      )
    }
  }
}

typealias LibraryItemDecorator = @Composable BoxScope.(isTransitioning: Boolean) -> Unit

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryItemCardImage(
  item: LibraryItem,
  sharedTransitionKey: String,
  sharedTransitionZIndex: Float,
  modifier: Modifier = Modifier,
  decorator: LibraryItemDecorator? = null,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  Box(
    modifier = modifier.clip(shape),
  ) {
    CoverImage(
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
      placeholder = painterResource(Res.drawable.placeholder_book),
      shape = shape,
      imageBitmapListener = rememberThemeDispatcherListener(item.id),
      modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .widthIn(max = CardMaxWidth)
        .clip(shape),
      sharedElementModifier = Modifier
        .thenIfNotNull(animationScope) { scope ->
          sharedElement(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Image,
              ),
            ),
            animatedVisibilityScope = scope,
            zIndexInOverlay = sharedTransitionZIndex,
          )
        },
    )

    val isTransitionVisible by remember {
      derivedStateOf {
        animationScope == null ||
          animationScope.transition.currentState == EnterExitState.Visible
      }
    }

    // Render decorators
    Box(
      modifier = Modifier
        .matchParentSize()
        .zIndex(1f),
    ) {
      decorator?.invoke(this, isTransitionVisible)
    }
  }
}

@Composable
private fun BoxScope.ProgressDecorator(
  progress: MediaProgress?,
  isTransitioning: Boolean,
  large: Boolean = true,
) {
  progress?.let { mediaProgress ->
    if (mediaProgress.isFinished) {
      AnimatedVisibility(
        visible = isTransitioning,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopStart),
      ) {
        MediaFinishedIndicator(
          size = if (large) 24.dp else 18.dp,
          modifier = Modifier
            .thenIf(
              condition = large,
              whenTrue = {
                Modifier.padding(8.dp)
              },
              whenFalse = {
                Modifier.padding(4.dp)
              },
            ),
        )
      }
    } else {
      AnimatedVisibility(
        visible = isTransitioning,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.BottomCenter),
      ) {
        MediaProgressBar(
          mediaProgress = mediaProgress,
          trackHeight = if (large) LargeProgressBarHeight else SmallProgressBarHeight,
          modifier = Modifier
            .fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun BoxScope.OfflineStatusDecorator(
  offlineStatus: OfflineStatus,
  isTransitioning: Boolean,
) {
  if (offlineStatus != OfflineStatus.None) {
    AnimatedVisibility(
      visible = isTransitioning,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(
          end = 8.dp,
          top = 8.dp,
        ),
    ) {
      OfflineStatusIndicator(
        status = offlineStatus,
      )
    }
  }
}

@Composable
private fun BoxScope.PodcastEpisodeDecorator(
  episode: PodcastEpisode,
  offlineStatus: OfflineStatus,
  isTransitioning: Boolean,
) {
  AnimatedVisibility(
    visible = isTransitioning,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier
      .align(Alignment.TopEnd)
      .padding(
        end = 8.dp,
        top = 8.dp,
      ),
  ) {
    Row(
      modifier = Modifier
        .clip(CircleShape)
        .background(
          color = MaterialTheme.colorScheme.scrim.copy(0.68f),
        )
        .padding(
          horizontal = 8.dp,
          vertical = 4.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (offlineStatus != OfflineStatus.None) {
        OfflineStatusIndicator(offlineStatus)
      }

      Text(
        text = "Episode ${episode.episode!!}",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
      )
    }
  }
}

@Composable
private fun BoxScope.EbookFormatDecorator(
  format: String,
  isTransitioning: Boolean,
) {
  AnimatedVisibility(
    visible = isTransitioning,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = Modifier
      .align(Alignment.TopEnd)
      .padding(
        end = 8.dp,
        top = 8.dp,
      ),
  ) {
    EbookFormatBadge(format = format)
  }
}

@Composable
fun EbookFormatBadge(
  format: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = format.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = Color.White,
    modifier = modifier
      .clip(CircleShape)
      .background(
        color = MaterialTheme.colorScheme.scrim.copy(0.68f),
      )
      .padding(
        horizontal = 8.dp,
        vertical = 4.dp,
      ),
  )
}

@Composable
private fun LibraryItemCardInformation(
  item: LibraryItem,
  sharedTransitionKey: String,
  modifier: Modifier = Modifier,
  marqueeEnabled: Boolean = true,
) {
  LibraryItemCardInformation(
    title = item.media.metadata.title,
    subtitle = item.media.metadata.authorName // Books
      ?: item.media.metadata.author // Podcasts
      ?: item.media.metadata.authors.firstOrNull()?.name, // Book fallback
    sharedTransitionKey = sharedTransitionKey,
    marqueeEnabled = marqueeEnabled,
    modifier = modifier,
  )
}

@Composable
private fun LibraryItemCardInformation(
  item: LibraryItem,
  episode: PodcastEpisode,
  sharedTransitionKey: String,
  modifier: Modifier = Modifier,
  marqueeEnabled: Boolean = true,
) {
  LibraryItemCardInformation(
    title = episode.title,
    subtitle = item.media.metadata.title,
    sharedTransitionKey = sharedTransitionKey,
    marqueeEnabled = marqueeEnabled,
    modifier = modifier,
  )
}

@Composable
private fun LibraryItemCardInformation(
  title: String?,
  subtitle: String?,
  sharedTransitionKey: String,
  modifier: Modifier = Modifier,
  marqueeEnabled: Boolean = true,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  val marqueeDelay = remember {
    Random.nextInt(1_200, 2_000)
  }

  val marqueeVelocity = remember {
    LibraryItemMarqueeVelocityRange.random().dp
  }

  Column(
    modifier.padding(
      vertical = 16.dp,
    ),
  ) {
    Text(
      text = title ?: stringResource(Res.string.unknown_library_title),
      style = MaterialTheme.typography.titleSmall,
      fontStyle = if (title == null) FontStyle.Italic else null,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .thenIfNotNull(animationScope) { scope ->
          sharedBounds(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Title,
              ),
            ),
            animatedVisibilityScope = scope,
          )
        }
        .thenIf(marqueeEnabled) {
          basicMarquee(
            velocity = marqueeVelocity,
            initialDelayMillis = marqueeDelay,
          )
        }
        .padding(horizontal = 16.dp),
    )
    Text(
      text = subtitle ?: stringResource(Res.string.unknown_author_name),
      style = MaterialTheme.typography.bodySmall,
      fontStyle = if (subtitle == null) FontStyle.Italic else null,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .thenIf(marqueeEnabled) {
          basicMarquee(
            velocity = marqueeVelocity,
            initialDelayMillis = marqueeDelay,
          )
        }
        .padding(horizontal = 16.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryItemCardEditingScrim(
  isSelectable: Boolean,
  selected: Boolean,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
) {
  AnimatedVisibility(
    visible = isSelectable,
    modifier = modifier,
  ) {
    val selectedBorderAlpha by animateFloatAsState(if (selected) 1f else 0.75f)
    val selectedBorderSize by animateDpAsState(if (selected) 3.dp else 1.dp)
    val selectedBackgroundAlpha by animateFloatAsState(if (selected) 0.75f else 0.3f)

    Box(
      modifier = Modifier
        .clip(shape)
        .fillMaxSize()
        .background(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = selectedBackgroundAlpha,
          ),
          shape = shape,
        )
        .border(
          width = selectedBorderSize,
          color = MaterialTheme.colorScheme.secondary.copy(
            alpha = selectedBorderAlpha,
          ),
          shape = shape,
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
fun MediaProgressBar(
  mediaProgress: MediaProgress,
  modifier: Modifier = Modifier,
  trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
  progressColor: Color = MaterialTheme.colorScheme.primary,
  trackHeight: Dp = LargeProgressBarHeight,
) {
  Canvas(
    modifier = modifier
      .height(trackHeight),
  ) {
    // Draw Track
    drawRect(
      color = trackColor,
      size = size,
      alpha = ProgressBarAlpha,
    )

    val cornerRadiusPx = trackHeight.toPx() / 2f
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
fun MediaFinishedIndicator(
  modifier: Modifier = Modifier,
  size: Dp = 24.dp,
  contentColor: Color = Color.Green,
  containerColor: Color = MaterialTheme.colorScheme.surface,
) {
  Box(
    modifier = modifier
      .shadow(
        elevation = 1.dp,
        shape = CircleShape,
      )
      .background(
        color = containerColor,
        shape = CircleShape,
      ),
  ) {
    Icon(
      CampfireIcons.Rounded.FatCheck,
      contentDescription = null,
      tint = contentColor,
      modifier = Modifier
        .size(size),
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

private val LibraryItemMarqueeVelocityRange = 30..40

val SmallProgressBarHeight = 8.dp
val LargeProgressBarHeight = 12.dp
private const val ProgressBarAlpha = 0.5f
