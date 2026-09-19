// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.sessions.ui.composables.ForwardIcon
import app.campfire.sessions.ui.composables.RewindIcon
import app.campfire.sessions.ui.playback.expanded.installPreviewComponents
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_play_pause
import campfire.features.sessions.ui.generated.resources.action_skip_next
import campfire.features.sessions.ui.generated.resources.action_skip_previous
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import org.jetbrains.compose.resources.stringResource

internal val MinButtonSize = 48.dp

data class PlaybackActionSize(
  val iconSize: Dp,
  val buttonSize: Dp,
  val buttonShapes: IconButtonShapes,
  val playButtonShapes: IconButtonShapes = buttonShapes,
) {

  companion object {

    @Composable
    fun large() = PlaybackActionSize(
      iconSize = ButtonDefaults.LargeIconSize,
      buttonSize = ButtonDefaults.LargeContainerHeight,
      buttonShapes = IconButtonDefaults.shapes(
        shape = MaterialTheme.shapes.extraLarge,
        pressedShape = MaterialTheme.shapes.medium,
      ),
      playButtonShapes = IconButtonDefaults.shapes(
        shape = MaterialTheme.shapes.extraLarge,
        pressedShape = CircleShape,
      ),
    )

    @Composable
    fun compact() = PlaybackActionSize(
      iconSize = ButtonDefaults.LargeIconSize,
      buttonSize = 80.dp,
      buttonShapes = IconButtonDefaults.shapes(
        shape = MaterialTheme.shapes.large,
        pressedShape = CircleShape,
      ),
      playButtonShapes = IconButtonDefaults.shapes(
        shape = MaterialTheme.shapes.largeIncreased,
        pressedShape = CircleShape,
      ),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CompactPlaybackActions(
  state: AudioPlayer.State,
  isInteracting: Boolean,
  onSkipPreviousClick: () -> Unit,
  onRewindClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: PlaybackActionSize = PlaybackActionSize.large(),
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
  ) {
    AccessoryButton(
      onClick = onSkipPreviousClick,
      shapes = size.buttonShapes,
      modifier = Modifier
        .sizeIn(
          minWidth = MinButtonSize,
          minHeight = size.buttonSize,
        ),
    ) {
      Icon(
        CampfireIcons.Rounded.SkipPrevious,
        modifier = Modifier.size(size.iconSize),
        contentDescription = stringResource(Res.string.action_skip_previous),
      )
    }

    AccessoryButton(
      onClick = onRewindClick,
      shapes = size.buttonShapes,
      modifier = Modifier
        .weight(1f)
        .sizeIn(minHeight = size.buttonSize),
    ) {
      RewindIcon(
        modifier = Modifier.size(size.iconSize),
      )
    }

    PlayPauseButton(
      state = state,
      isInteracting = isInteracting,
      onClick = onPlayPauseClick,
      shapes = size.playButtonShapes,
      modifier = Modifier
        .sizeIn(minHeight = size.buttonSize)
        .weight(1.75f)
        .padding(horizontal = 2.dp),
    )

    AccessoryButton(
      onClick = onForwardClick,
      shapes = size.buttonShapes,
      modifier = Modifier
        .weight(1f)
        .sizeIn(minHeight = size.buttonSize),
    ) {
      ForwardIcon(
        modifier = Modifier.size(size.iconSize),
      )
    }

    AccessoryButton(
      onClick = onSkipNextClick,
      shapes = size.buttonShapes,
      modifier = Modifier
        .sizeIn(
          minWidth = MinButtonSize,
          minHeight = size.buttonSize,
        ),
    ) {
      Icon(
        CampfireIcons.Rounded.SkipNext,
        modifier = Modifier.size(size.iconSize),
        contentDescription = stringResource(Res.string.action_skip_next),
      )
    }
  }
}

@Composable
private fun AccessoryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shapes: IconButtonShapes = IconButtonDefaults.shapes(),
  content: @Composable () -> Unit,
) {
  val accessoryButtonContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
  val accessoryButtonContentColor = MaterialTheme.colorScheme.onSurface
  FilledIconButton(
    onClick = onClick,
    shapes = shapes,
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = accessoryButtonContainerColor,
      contentColor = accessoryButtonContentColor,
    ),
    modifier = modifier,
    content = content,
  )
}

@Composable
private fun PlayPauseButton(
  state: AudioPlayer.State,
  isInteracting: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = ButtonDefaults.ExtraLargeIconSize,
  shapes: IconButtonShapes = IconButtonDefaults.shapes(),
) {
  val isPlayPauseEnabled = state != AudioPlayer.State.Finished &&
    state != AudioPlayer.State.Buffering &&
    !isInteracting

  FilledIconButton(
    onClick = onClick,
    enabled = isPlayPauseEnabled,
    shapes = shapes,
    modifier = modifier,
  ) {
    AnimatedContent(
      targetState = when {
        state == AudioPlayer.State.Buffering -> PlayButtonState.Buffering
        isInteracting -> PlayButtonState.Interacting
        state == AudioPlayer.State.Playing -> PlayButtonState.Playing
        else -> PlayButtonState.Paused
      },
      transitionSpec = {
        (fadeIn(initialAlpha = 0.4f) + expandIn(expandFrom = Alignment.Center)) togetherWith
          (fadeOut(targetAlpha = 0.4f) + shrinkOut(shrinkTowards = Alignment.Center))
      },
      contentAlignment = Alignment.Center,
    ) { state ->
      if (state == PlayButtonState.Buffering) {
        CircularProgressIndicator(
          modifier = Modifier.size(iconSize),
          strokeWidth = 4.dp,
        )
      } else {
        Icon(
          when (state) {
            PlayButtonState.Interacting -> CampfireIcons.Rounded.EditAudio
            PlayButtonState.Playing -> CampfireIcons.Rounded.Pause
            else -> CampfireIcons.Rounded.PlayArrow
          },
          modifier = Modifier.size(iconSize),
          contentDescription = stringResource(Res.string.action_play_pause),
        )
      }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PreviewCompactPlaybackActions(
  playbackActionSize: PlaybackActionSize = PlaybackActionSize.large(),
) {
  installPreviewComponents()
  CampfireTheme(useDarkColors = false) {
    PreviewSharedElementTransitionLayout {
      Surface(Modifier.fillMaxWidth()) {
        Box(
          modifier = Modifier.padding(16.dp),
        ) {
          CompactPlaybackActions(
            state = AudioPlayer.State.Paused,
            isInteracting = false,
            onSkipPreviousClick = {},
            onRewindClick = {},
            onPlayPauseClick = {},
            onForwardClick = {},
            onSkipNextClick = {},
            size = playbackActionSize,
          )
        }
      }
    }
  }
}

@Preview
@Composable
fun CompactPlaybackActionsPreview() {
  PreviewCompactPlaybackActions()
}

@Preview
@Composable
fun Small_CompactPlaybackActionsPreview() {
  PreviewCompactPlaybackActions(
    PlaybackActionSize.compact(),
  )
}
