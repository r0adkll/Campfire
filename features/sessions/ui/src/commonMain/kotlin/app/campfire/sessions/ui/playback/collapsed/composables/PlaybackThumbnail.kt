package app.campfire.sessions.ui.playback.collapsed.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.sessions.ui.composables.Thumbnail
import app.campfire.sessions.ui.playback.collapsed.ActionState.Dispose
import app.campfire.sessions.ui.playback.collapsed.PlaybackBarDragState
import app.campfire.sessions.ui.playback.SharedImage

@Composable
internal fun SharedTransitionScope.PlaybackThumbnail(
  thumbnailUrl: String?,
  thumbnailContentDescription: String?,
  animatedVisibilityScope: AnimatedVisibilityScope,
  runningTimer: RunningTimer?,
  dragState: PlaybackBarDragState,
) {
  Box(
    modifier = Modifier.padding(4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Thumbnail(
      imageUrl = thumbnailUrl,
      contentDescription = thumbnailContentDescription,
      modifier = Modifier
        .sharedElement(
          rememberSharedContentState(SharedImage),
          animatedVisibilityScope = animatedVisibilityScope,
        ),
    )

    // Sleep / Snooze Icon
    AnimatedVisibleIcon(
      visible = runningTimer != null && dragState.actionState != Dispose,
      imageVector = Icons.Rounded.Snooze,
    )

    // Delete/Dispose Icon
    AnimatedVisibleIcon(
      visible = dragState.actionState == Dispose,
      imageVector = Icons.Rounded.DeleteSweep,
    )
  }
}

@Composable
private fun AnimatedVisibleIcon(
  visible: Boolean,
  imageVector: ImageVector,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier,
  ) {
    val cornerRadius by transition.animateDp {
      if (it == EnterExitState.Visible) 8.dp else 28.dp
    }

    val size by transition.animateDp {
      if (it == EnterExitState.Visible) 56.dp else 0.dp
    }

    Box(
      modifier = Modifier
        .background(
          color = MaterialTheme.colorScheme.error.copy(0.6f),
          shape = RoundedCornerShape(cornerRadius),
        )
        .size(size),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector,
        contentDescription = null,
        tint = Color.White,
      )
    }
  }
}
