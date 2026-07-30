// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.add.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.podcasts.api.PodcastSearchResult
import app.campfire.podcasts.ui.AddPodcastSharedTransitionKey
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_episode_count
import campfire.features.podcasts.ui.generated.resources.add_podcast_result_cover
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

internal val ThumbnailSize = 88.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PodcastSearchResultRow(
  result: PodcastSearchResult,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String = result.feedUrl,
  shape: Shape = MaterialTheme.shapes.large,
) = SharedElementTransitionScope {
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

  ElevatedCard(
    modifier = modifier
      .fillMaxWidth()
      .thenIfNotNull(animationScope) { scope ->
        sharedBounds(
          sharedContentState = rememberSharedContentState(
            AddPodcastSharedTransitionKey(
              id = sharedTransitionKey,
              type = AddPodcastSharedTransitionKey.ElementType.Bounds,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
    shape = shape,
    onClick = onClick,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val cover = result.coverUrl
      if (!cover.isNullOrBlank()) {
        CoverImage(
          imageUrl = cover,
          contentDescription = stringResource(Res.string.add_podcast_result_cover, result.title),
          shape = shape,
          modifier = Modifier
            .size(ThumbnailSize)
            .clip(shape),
          sharedElementModifier = Modifier.thenIfNotNull(animationScope) { scope ->
            sharedElement(
              sharedContentState = rememberSharedContentState(
                AddPodcastSharedTransitionKey(
                  id = sharedTransitionKey,
                  type = AddPodcastSharedTransitionKey.ElementType.Cover,
                ),
              ),
              animatedVisibilityScope = scope,
            )
          },
        )
      }

      Spacer(Modifier.width(16.dp))

      Column(
        modifier = Modifier
          .weight(1f),
      ) {
        Text(
          text = result.title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val author = result.author
        if (!author.isNullOrBlank()) {
          Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        result.trackCount?.let { count ->
          Spacer(Modifier.height(2.dp))
          Text(
            text = stringResource(Res.string.add_podcast_episode_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(Modifier.width(16.dp))
    }
  }
}
