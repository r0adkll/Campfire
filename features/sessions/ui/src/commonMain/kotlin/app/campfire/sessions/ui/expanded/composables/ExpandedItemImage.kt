package app.campfire.sessions.ui.expanded.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ShakeVeryHigh
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.core.model.Session
import app.campfire.sessions.ui.SharedImage
import app.campfire.sessions.ui.composables.RunningTimerText

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.ExpandedItemImage(
  session: Session,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,
  animatedVisibilityScope: AnimatedVisibilityScope,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier,
  ) {
    val mediaUrl = currentMetadata.artworkUri
      ?: session.libraryItem.media.coverImageUrl
    CoverImage(
      imageUrl = mediaUrl,
      contentDescription = session.libraryItem.media.metadata.title,
      size = size,
      modifier = Modifier.sharedElement(
        rememberSharedContentState(SharedImage),
        animatedVisibilityScope = animatedVisibilityScope,
      ),
    )

    AnimatedVisibility(
      visible = runningTimer != null,
      enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
      modifier = Modifier.size(size),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(size)
          .background(Color.Black.copy(0.3f), RoundedCornerShape(32.dp)),
      ) {
        if (runningTimer?.isShakeToRestartEnabled == true) {
          Icon(
            CampfireIcons.Rounded.ShakeVeryHigh,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(16.dp),
          )
        }

        if (runningTimer != null) {
          RunningTimerText(
            runningTimer = runningTimer,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
      }
    }
  }
}
