// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.podcasts.ui.AddPodcastSharedTransitionKey
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_result_cover
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CoverPreview(
  coverUrl: String?,
  title: String,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String? = null,
) = SharedElementTransitionScope {
  if (coverUrl.isNullOrBlank()) return@SharedElementTransitionScope
  val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)
  val transitionScope = sharedTransitionKey?.let { animationScope }

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    val size = minWidth * 0.75f
    CoverImage(
      imageUrl = coverUrl,
      contentDescription = stringResource(Res.string.add_podcast_result_cover, title),
      size = size,
      modifier = Modifier
        .size(300.dp)
        .clip(MaterialTheme.shapes.large),
      sharedElementModifier = Modifier.thenIfNotNull(transitionScope) { scope ->
        sharedElement(
          sharedContentState = rememberSharedContentState(
            AddPodcastSharedTransitionKey(
              id = sharedTransitionKey!!,
              type = AddPodcastSharedTransitionKey.ElementType.Cover,
            ),
          ),
          animatedVisibilityScope = scope,
        )
      },
    )
  }
}
