// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.BookReview
import app.campfire.bookinfo.api.CommunityContent
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.common.compose.extensions.rememberHtmlRichTextState
import app.campfire.common.compose.extensions.roundToSingleDecimal
import app.campfire.common.compose.extensions.thenIf
import app.campfire.common.compose.icons.providerBrandIcon
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ReviewBottomSheet
import app.campfire.libraries.ui.detail.composables.ReviewerAvatar
import app.campfire.libraries.ui.detail.composables.ReviewerBadge
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.community_connect_for_reviews
import campfire.features.libraries.ui.generated.resources.community_empty_source
import campfire.features.libraries.ui.generated.resources.community_rating_count
import campfire.features.libraries.ui.generated.resources.community_relink_provider
import campfire.features.libraries.ui.generated.resources.community_review_show_spoiler
import campfire.features.libraries.ui.generated.resources.community_review_spoiler_warning
import campfire.features.libraries.ui.generated.resources.community_title
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.ui.material.RichText
import org.jetbrains.compose.resources.stringResource

/**
 * Combined community section: aggregate rating and reviews from one third-party
 * source, with a source pill in the header that switches provider when more
 * than one can serve this book.
 */
class CommunitySlot(
  private val state: CommunityInfoState,
) : ContentSlot {

  override val id: String = "community"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    var expandedReview by remember { mutableStateOf<BookReview?>(null) }

    Column(
      modifier = modifier,
    ) {
      // The header (and its source pill) renders in every phase so the section
      // stays put while a source switch or first fetch resolves.
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .padding(horizontal = 16.dp),
      ) {
        MetadataHeader(
          title = stringResource(Res.string.community_title),
          textStyle = MaterialTheme.typography.titleLarge,
          textColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        SourcePill(state = state, eventSink = eventSink)
      }

      when (val content = state.content) {
        CommunityContent.Loading -> PhaseBox {
          CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }

        CommunityContent.Unavailable -> PhaseBox {
          Text(
            text = stringResource(Res.string.community_empty_source, state.providerName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }

        is CommunityContent.Available -> RatingContent(
          content = content,
          onOpenProvider = { url -> eventSink(LibraryItemUiEvent.OpenProviderPage(url)) },
        )
      }

      if (state.needsRelink) {
        TextButton(
          onClick = { eventSink(LibraryItemUiEvent.RelinkProvider) },
          modifier = Modifier.padding(horizontal = 8.dp),
        ) {
          Text(stringResource(Res.string.community_relink_provider, state.providerName))
        }
      }

      val reviews = (state.content as? CommunityContent.Available)?.reviews.orEmpty()
      if (reviews.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(reviews) { review ->
            ReviewCard(
              review = review,
              providerKey = state.providerId.key,
              onClick = { expandedReview = review },
            )
          }
        }
      }

      // Serving source has no review text, but linking another provider adds it.
      if (state.content !is CommunityContent.Loading && reviews.isEmpty()) {
        state.reviewsLinkProviderName?.let { linkName ->
          TextButton(
            onClick = { eventSink(LibraryItemUiEvent.RelinkProvider) },
            modifier = Modifier.padding(horizontal = 8.dp),
          ) {
            Text(stringResource(Res.string.community_connect_for_reviews, linkName))
          }
        }
      }
    }

    expandedReview?.let { review ->
      ReviewBottomSheet(
        review = review,
        state = state,
        onOpenProvider = { url ->
          expandedReview = null
          eventSink(LibraryItemUiEvent.OpenProviderPage(url))
        },
        onDismiss = { expandedReview = null },
      )
    }
  }
}

/**
 * Fixed-height container for the loading and empty phases, sized to the rating
 * row so switching phases doesn't shift the page.
 */
@Composable
private fun PhaseBox(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 68.dp)
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    content()
  }
}

@Composable
private fun RatingContent(
  content: CommunityContent.Available,
  onOpenProvider: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rating = content.info.rating ?: return
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .thenIf(content.info.providerUrl != null) {
        clickable {
          onOpenProvider(content.info.providerUrl!!)
        }
      },
  ) {
    Spacer(Modifier.width(16.dp))

    Text(
      text = rating.roundToSingleDecimal(),
      style = MaterialTheme.typography.headlineLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.width(16.dp))

    Column(
      modifier = Modifier
        .padding(vertical = 12.dp),
    ) {
      StarRow(rating = rating)

      Spacer(Modifier.height(2.dp))

      content.info.ratingsCount?.let { count ->
        Text(
          text = stringResource(Res.string.community_rating_count, count.toString()),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.width(16.dp))
  }
}

@Composable
private fun SourcePill(
  state: CommunityInfoState,
  eventSink: (LibraryItemUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val canSwitch = state.availableSources.size > 1

  Box(modifier = modifier) {
    Surface(
      onClick = { expanded = true },
      enabled = canSwitch,
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
          start = 12.dp,
          end = if (canSwitch) 6.dp else 12.dp,
          top = 6.dp,
          bottom = 6.dp,
        ),
      ) {
        providerBrandIcon(state.providerId.key)?.let { brandIcon ->
          Image(
            imageVector = brandIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(4.dp))
        }
        Text(
          text = state.providerName,
          style = MaterialTheme.typography.labelMedium,
        )
        if (canSwitch) {
          Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      shape = MaterialTheme.shapes.medium,
    ) {
      state.availableSources.forEach { source ->
        DropdownMenuItem(
          text = { Text(source.name) },
          leadingIcon = {
            providerBrandIcon(source.id.key)?.let { brandIcon ->
              Image(
                imageVector = brandIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
            }
          },
          trailingIcon = if (source.id == state.providerId) {
            {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          } else {
            null
          },
          onClick = {
            expanded = false
            eventSink(LibraryItemUiEvent.SelectCommunitySource(source.id))
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun ReviewCard(
  review: BookReview,
  providerKey: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var spoilerRevealed by remember(review) { mutableStateOf(false) }
  val obscured = review.hasSpoilers && !spoilerRevealed

  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.width(280.dp),
  ) {
    Box {
      Column(
        modifier = Modifier.padding(16.dp),
      ) {
        ReviewHeader(review = review, providerKey = providerKey)
        Spacer(Modifier.height(8.dp))
        // Every card measures five text lines tall regardless of review
        // length; a titled review trades one body line for its headline.
        review.title?.let { title ->
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (obscured) Modifier.blur(12.dp) else Modifier,
          )
        }
        val bodyLines = if (review.title != null) 4 else 5
        RichText(
          state = rememberHtmlRichTextState(review.text),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          minLines = bodyLines,
          maxLines = bodyLines,
          overflow = TextOverflow.Ellipsis,
          modifier = if (obscured) Modifier.blur(12.dp) else Modifier,
        )
      }

      if (obscured) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp),
        ) {
          Text(
            text = stringResource(Res.string.community_review_spoiler_warning),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          TextButton(onClick = { spoilerRevealed = true }) {
            Text(stringResource(Res.string.community_review_show_spoiler))
          }
        }
      }
    }
  }
}

@Composable
private fun ReviewHeader(
  review: BookReview,
  providerKey: String,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth(),
  ) {
    // Avatar always renders (monogram fallback) and the badge sits inline with
    // the name, so every card header measures the same height.
    ReviewerAvatar(url = review.avatarUrl, fallbackName = review.author)
    Spacer(Modifier.width(8.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      review.author?.let { author ->
        Text(
          text = author,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
      }
      review.badge?.let { badge ->
        Spacer(Modifier.width(6.dp))
        ReviewerBadge(text = badge, providerKey = providerKey)
      }
    }
    Spacer(Modifier.width(8.dp))
    review.rating?.let { rating ->
      Icon(
        imageVector = Icons.Rounded.Star,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(2.dp))
      Text(
        text = rating.roundToSingleDecimal(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StarRow(
  rating: Double,
  modifier: Modifier = Modifier,
) {
  val layoutDirection = LocalLayoutDirection.current
  Row(modifier = modifier) {
    repeat(5) { index ->
      val fill = starFillFraction(rating, index)
      Box(
        modifier = Modifier.size(18.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Star,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
          modifier = Modifier.matchParentSize(),
        )
        if (fill > 0f) {
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .matchParentSize()
              .drawWithContent {
                val fillWidth = size.width * fill
                val left = when (layoutDirection) {
                  LayoutDirection.Rtl -> size.width - fillWidth
                  LayoutDirection.Ltr -> 0f
                }
                clipRect(left = left, right = left + fillWidth) {
                  this@drawWithContent.drawContent()
                }
              },
          )
        }
      }
    }
  }
}

/** How much of the star at [index] (0-based) is filled for [rating], in 0f..1f. */
internal fun starFillFraction(rating: Double, index: Int): Float {
  return (rating - index).toFloat().coerceIn(0f, 1f)
}
