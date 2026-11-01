// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.offline.OfflineStatus
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.item_actions_label
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryItemListItem(
  libraryItem: LibraryItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailingContent: @Composable () -> Unit = {},
  mediaProgress: MediaProgress? = libraryItem.userMediaProgress,
  offlineStatus: OfflineStatus = OfflineStatus.None,
  // How many of a podcast's episodes the user hasn't finished, badged on the cover when above 0
  unfinishedEpisodes: Int = 0,
  sharedTransitionKey: String = libraryItem.id,
  sharedTransitionZIndex: Float = 0f,
  // Optional overrides used when the row should display something other than the parent
  // library item (e.g. a podcast episode within a playlist row).
  titleOverride: String? = null,
  subtitleOverride: String? = null,
  // The facts shown on the bottom line, such as the item's length
  meta: List<LibraryItemMeta> = listOf(libraryItem.defaultMeta),
  // An extra line under the subtitle, such as the series the item belongs to
  supportingText: String? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  // Whether a long press or a right click offers the item's actions, for rows that stand for the
  // item itself rather than, say, a playlist's entry, which may be one of a podcast's episodes
  offersActions: Boolean = false,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)
  val shape = MaterialTheme.shapes.large
  val openActions = if (offersActions) rememberLibraryItemActionsOpener(libraryItem) else null
  val actionsLabel = stringResource(Res.string.item_actions_label)
  val colors = CardDefaults.elevatedCardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
  )
  val cardModifier = modifier
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
    }
    .onRightClick(openActions?.let { open -> { pointer -> open(pointer) } })
  val content: @Composable ColumnScope.() -> Unit = {
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

        if (libraryItem.isEbookOnly) {
          EbookFormatBadge(
            format = libraryItem.media.ebookFormat.orEmpty(),
            offlineStatus = offlineStatus,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
          )
        } else if (unfinishedEpisodes > 0) {
          UnfinishedEpisodesBadge(
            count = unfinishedEpisodes,
            offlineStatus = offlineStatus,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp),
          )
        } else {
          OfflineStatusIndicator(
            status = offlineStatus,
            size = 20.dp,
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

        if (supportingText != null) {
          Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(Modifier.size(4.dp))

        LibraryItemMetaRow(meta = meta)
      }

      trailingContent()

      Spacer(Modifier.size(8.dp))
    }
  }

  if (openActions != null) {
    // The card's own click has no long press, so it's handled around a card without one
    ElevatedCard(
      colors = colors,
      shape = shape,
      modifier = cardModifier
        .clip(shape)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = ripple(),
          role = Role.Button,
          onLongClickLabel = actionsLabel,
          onLongClick = { openActions(null) },
          onClick = onClick,
        ),
      content = content,
    )
  } else {
    ElevatedCard(
      onClick = onClick,
      interactionSource = interactionSource,
      colors = colors,
      shape = shape,
      modifier = cardModifier,
      content = content,
    )
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
