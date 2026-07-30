// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.placeholder_book
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalSharedTransitionApi::class)
class CoverImageSlot(
  private val imageUrl: String?,
  private val contentDescription: String?,
  private val sharedTransitionKey: String,
) : ContentSlot {

  override val id: String = "cover_image"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) = SharedElementTransitionScope {
    BoxWithConstraints(
      modifier = modifier.fillMaxWidth(),
    ) {
      val size = minWidth * 0.75f
      CoverImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        size = size,
        placeholder = painterResource(Res.drawable.placeholder_book),
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            vertical = 16.dp,
          ),
        sharedElementModifier = Modifier
          .sharedElement(
            sharedContentState = rememberSharedContentState(
              LibraryItemSharedTransitionKey(
                id = sharedTransitionKey,
                type = LibraryItemSharedTransitionKey.ElementType.Image,
              ),
            ),
            animatedVisibilityScope = requireAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation),
          ),
      )
    }
  }
}
