// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.offline.OfflineStatus
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import kotlin.time.Duration

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryItemListItem(
  libraryItem: LibraryItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable () -> Unit = {},
  mediaProgress: MediaProgress? = libraryItem.userMediaProgress,
  offlineStatus: OfflineStatus = OfflineStatus.None,
  sharedTransitionKey: String = libraryItem.id,
  sharedTransitionZIndex: Float = 0f,
  // Optional overrides used when the row should display something other than the parent
  // library item (e.g. a podcast episode within a playlist row).
  titleOverride: String? = null,
  subtitleOverride: String? = null,
  durationOverride: Duration? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)
  val shape = MaterialTheme.shapes.large
  ElevatedCard(
    onClick = onClick,
    interactionSource = interactionSource,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    shape = shape,
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
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .clip(shape)
          .size(ThumbnailSize),
      ) {
        val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

        ItemImage(
          imageUrl = libraryItem.media.coverImageUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
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
            }
            .clip(shape)
            .size(ThumbnailSize),
        )

        OfflineStatusIndicator(
          status = offlineStatus,
          size = 20.dp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        )

        if (libraryItem.isEbookOnly) {
          EbookFormatBadge(
            format = libraryItem.media.ebookFormat.orEmpty(),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
          )
        }

        if (mediaProgress?.isFinished == true) {
          MediaFinishedIndicator(
            size = 18.dp,
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp),
          )
        }

        mediaProgress
          ?.takeUnless { it.isFinished }
          ?.let { progress ->
            MediaProgressBar(
              mediaProgress = progress,
              trackHeight = SmallProgressBarHeight,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            )
          }
      }

      Spacer(Modifier.size(16.dp))

      Column(
        modifier = Modifier
          .weight(1f),
      ) {
        Text(
          text = titleOverride ?: libraryItem.media.metadata.title ?: "Unknown",
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        // For podcast metadata `authorName` is null (the field is `author`); fall back so
        // both books and podcasts render a sensible attribution line.
        val defaultSubtitle = libraryItem.media.metadata.authorName
          ?: libraryItem.media.metadata.author
          ?: "--"
        Text(
          text = subtitleOverride ?: defaultSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.size(4.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = (durationOverride ?: libraryItem.media.duration).thresholdReadoutFormat(),
            style = MaterialTheme.typography.labelSmallEmphasized,
          )
        }
      }

      trailingContent()

      Spacer(Modifier.size(8.dp))
    }
  }
}

private val ThumbnailSize = 88.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun LibraryItemListItemPreview() {
  CampfireTheme {
    PreviewSharedElementTransitionLayout {
      Scaffold {
        Column {
          LibraryItemListItem(
            libraryItem = libraryItem("item_id"),
            onClick = {},
          )

          LibraryItemListItem(
            libraryItem = libraryItem(
              id = "item_id2",
              userMediaProgress = MediaProgress(
                id = "",
                userId = "",
                libraryItemId = "",
                mediaItemId = "",
                progress = 0.6f,
                hideFromContinueListening = false,
                isFinished = false,
                ebookLocation = null,
                mediaItemType = MediaType.Book,
                duration = 100f,
                currentTime = 60f,
                lastUpdate = 0,
                startedAt = 0,
                source = MediaProgress.Source.Local,
              ),
            ),
            onClick = {},
          )
        }
      }
    }
  }
}
