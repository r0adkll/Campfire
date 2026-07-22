@file:OptIn(ExperimentalSharedTransitionApi::class)

package app.campfire.common.compose.widgets

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIf
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.core.model.LibraryItem
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.filter_bar_book_count
import campfire.common.compose.generated.resources.placeholder_book
import coil3.compose.rememberAsyncImagePainter
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource

private val CardMaxWidth = 400.dp
private val MosaicGap = 2.dp

/** Maximum number of covers rendered within the [CollectionCoverStyle.Mosaic] cover. */
const val MaxMosaicCovers = 4

/**
 * How the cover art for an [ItemCollectionGridCard] is rendered.
 */
enum class CollectionCoverStyle {
  /**
   * A dynamic mosaic of up to [MaxMosaicCovers] covers that adapts its layout to the number
   * of items (1 = single, 2 = split, 3 = feature + stack, 4+ = 2x2 grid).
   */
  Mosaic,

  /**
   * A single cover using the first item in the collection, matching [LibraryItemCard].
   */
  FirstItem,
}

/**
 * A grid-friendly variant of [ItemCollectionCard] for representing a collection, series, or playlist
 * as a single square card, visually consistent with [LibraryItemCard] but able to gracefully
 * represent the multiple items contained within it.
 *
 * A count badge is drawn in the corner of the cover whenever the collection holds more than one item.
 *
 * @param name the collection's display name, shown as the card title.
 * @param items the items contained in the collection; their covers drive the cover art.
 * @param onClick invoked when the card is tapped.
 * @param subtitle an optional secondary line; when null a "N Books" count label is shown instead.
 * @param coverStyle whether to render a dynamic [CollectionCoverStyle.Mosaic] of covers or just the
 *   [CollectionCoverStyle.FirstItem] cover.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ItemCollectionGridCard(
  name: String,
  items: List<LibraryItem>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  sharedTransitionKey: String = name,
  coverStyle: CollectionCoverStyle = CollectionCoverStyle.Mosaic,
  marqueeEnabled: Boolean = LocalItemCardMarquee.current,
  showInformation: Boolean = true,
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
            ItemCollectionSharedTransitionKey(
              id = sharedTransitionKey,
              type = ItemCollectionSharedTransitionKey.ElementType.Bounds,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
    onClick = onClick,
    colors = colors,
    shape = shape,
  ) {
    Box(
      modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth()
        .widthIn(max = CardMaxWidth)
        .clip(shape),
    ) {
      when (coverStyle) {
        CollectionCoverStyle.Mosaic -> MosaicCover(
          items = items,
          sharedTransitionKeyModifier = sharedTransitionKey,
          modifier = Modifier.fillMaxSize(),
        )
        CollectionCoverStyle.FirstItem -> FirstItemCover(
          item = items.firstOrNull(),
          sharedTransitionKeyModifier = sharedTransitionKey,
          modifier = Modifier.fillMaxSize(),
        )
      }

      CollectionCountBadge(
        count = items.size,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
      )
    }

    if (showInformation) {
      ItemCollectionInformation(
        title = name,
        subtitle = subtitle,
        itemCount = items.size,
        sharedTransitionKey = sharedTransitionKey,
        marqueeEnabled = marqueeEnabled,
      )
    }
  }
}

@Composable
internal fun BoxScope.CollectionCountBadge(
  count: Int,
  modifier: Modifier = Modifier,
) {
  if (count <= 1) return

  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(color = MaterialTheme.colorScheme.scrim.copy(0.68f))
      .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
      .padding(horizontal = 8.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "$count",
      style = MaterialTheme.typography.labelMedium,
      color = Color.White,
    )
  }
}

@Composable
private fun FirstItemCover(
  item: LibraryItem?,
  sharedTransitionKeyModifier: String,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  CoverCell(
    imageUrl = item?.media?.coverImageUrl,
    contentDescription = item?.media?.metadata?.title,
    modifier = modifier,
    sharedElementModifier = Modifier
      .thenIfNotNull(item?.let { animationScope }) { scope ->
        sharedElement(
          sharedContentState = rememberSharedContentState(
            LibraryItemSharedTransitionKey(
              id = item!!.id + sharedTransitionKeyModifier,
              type = LibraryItemSharedTransitionKey.ElementType.Image,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
  )
}

@Composable
private fun MosaicCover(
  items: List<LibraryItem>,
  sharedTransitionKeyModifier: String,
  modifier: Modifier = Modifier,
) {
  val covers = remember(items) { items.take(MaxMosaicCovers) }

  when (covers.size) {
    0 -> CoverCell(imageUrl = null, contentDescription = null, modifier = modifier)
    1 -> MosaicCell(covers[0], sharedTransitionKeyModifier, modifier = modifier)
    2 -> Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(MosaicGap),
    ) {
      covers.forEach { item ->
        MosaicCell(
          item = item,
          sharedTransitionKeyModifier = sharedTransitionKeyModifier,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        )
      }
    }
    3 -> Row(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(MosaicGap),
    ) {
      // Feature the first cover on the left, stack the remaining two on the right
      MosaicCell(
        item = covers[0],
        sharedTransitionKeyModifier = sharedTransitionKeyModifier,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(MosaicGap),
      ) {
        covers.drop(1).forEach { item ->
          MosaicCell(
            item = item,
            sharedTransitionKeyModifier = sharedTransitionKeyModifier,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          )
        }
      }
    }
    else -> Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(MosaicGap),
    ) {
      // 2x2 grid
      for (rowIndex in 0 until 2) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(MosaicGap),
        ) {
          for (columnIndex in 0 until 2) {
            val item = covers[rowIndex * 2 + columnIndex]
            MosaicCell(
              item = item,
              sharedTransitionKeyModifier = sharedTransitionKeyModifier,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MosaicCell(
  item: LibraryItem,
  sharedTransitionKeyModifier: String,
  modifier: Modifier = Modifier,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  CoverCell(
    imageUrl = item.media.coverImageUrl,
    contentDescription = item.media.metadata.title,
    modifier = modifier,
    sharedElementModifier = Modifier
      .thenIfNotNull(animationScope) { scope ->
        sharedElement(
          sharedContentState = rememberSharedContentState(
            LibraryItemSharedTransitionKey(
              id = item.id + sharedTransitionKeyModifier,
              type = LibraryItemSharedTransitionKey.ElementType.Image,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
  )
}

@Composable
private fun CoverCell(
  imageUrl: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  sharedElementModifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
  ) {
    val painter = key(imageUrl) {
      rememberAsyncImagePainter(
        model = imageUrl,
        error = painterResource(Res.drawable.placeholder_book),
      )
    }

    Image(
      painter = painter,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = sharedElementModifier.fillMaxSize(),
    )
  }
}

@Composable
private fun ItemCollectionInformation(
  title: String,
  subtitle: String?,
  itemCount: Int,
  sharedTransitionKey: String,
  modifier: Modifier = Modifier,
  marqueeEnabled: Boolean = true,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  Column(
    modifier.padding(vertical = 16.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .thenIfNotNull(animationScope) { scope ->
          sharedBounds(
            sharedContentState = rememberSharedContentState(
              ItemCollectionSharedTransitionKey(
                id = sharedTransitionKey,
                type = ItemCollectionSharedTransitionKey.ElementType.Title,
              ),
            ),
            animatedVisibilityScope = scope,
          )
        }
        .thenIf(marqueeEnabled) {
          basicMarquee()
        }
        .padding(horizontal = 16.dp),
    )

    Text(
      text = subtitle
        ?: pluralStringResource(Res.plurals.filter_bar_book_count, itemCount, itemCount),
      style = MaterialTheme.typography.bodySmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .thenIf(marqueeEnabled) {
          basicMarquee()
        }
        .padding(horizontal = 16.dp),
    )
  }
}
