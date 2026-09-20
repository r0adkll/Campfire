// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.sessions.ui.composables.ForwardIcon
import app.campfire.sessions.ui.composables.RewindIcon
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_play_pause
import campfire.features.sessions.ui.generated.resources.action_skip_next
import campfire.features.sessions.ui.generated.resources.action_skip_previous
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaybackActions(
  state: AudioPlayer.State,
  isInteracting: Boolean,
  onSkipPreviousClick: () -> Unit,
  onRewindClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = ButtonDefaults.LargeContainerHeight,
  buttonShape: Shape = MaterialTheme.shapes.extraLarge,
  playButtonExtraWidth: Dp = 32.dp,
  playButtonIconSize: Dp = ButtonDefaults.ExtraLargeIconSize,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    val accessoryButtonContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val accessoryButtonContentColor = MaterialTheme.colorScheme.onSurface

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
      AccessoryButton(
        onClick = onRewindClick,
        buttonSize = DpSize(buttonSize, buttonSize),
        buttonShape = buttonShape,
      ) { iconSize ->
        RewindIcon(
          modifier = Modifier.size(iconSize),
        )
      }

      PlayPauseButton(
        state = state,
        isInteracting = isInteracting,
        onClick = onPlayPauseClick,
        extraWidth = playButtonExtraWidth,
        iconSize = playButtonIconSize,
        buttonSize = buttonSize,
        buttonShape = buttonShape,
        modifier = Modifier.padding(horizontal = 2.dp),
      )

      AccessoryButton(
        onClick = onForwardClick,
        buttonSize = DpSize(buttonSize, buttonSize),
        buttonShape = buttonShape,
      ) { iconSize ->
        ForwardIcon(
          modifier = Modifier.size(iconSize),
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .widthIn(
          max = (buttonSize * 3) + playButtonExtraWidth + 8.dp,
        ),
    ) {
      val buttonSize = ButtonDefaults.MinHeight
      val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = accessoryButtonContainerColor,
        contentColor = accessoryButtonContentColor,
      )
      val shapes = IconButtonDefaults.shapes()

      FilledIconButton(
        onClick = onSkipPreviousClick,
        shapes = shapes,
        colors = colors,
        modifier = Modifier
          .weight(1f)
          .heightIn(buttonSize),
      ) {
        Icon(
          CampfireIcons.Rounded.SkipPrevious,
          modifier = Modifier.size(ButtonDefaults.LargeIconSize),
          contentDescription = stringResource(Res.string.action_skip_previous),
        )
      }
      FilledIconButton(
        onClick = onSkipNextClick,
        shapes = shapes,
        colors = colors,
        modifier = Modifier
          .weight(1f)
          .heightIn(buttonSize),
      ) {
        Icon(
          CampfireIcons.Rounded.SkipNext,
          modifier = Modifier.size(ButtonDefaults.LargeIconSize),
          contentDescription = stringResource(Res.string.action_skip_next),
        )
      }
    }
  }
}

@Composable
private fun AccessoryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: DpSize = DpSize(ButtonDefaults.LargeContainerHeight, ButtonDefaults.LargeContainerHeight),
  buttonShape: Shape = MaterialTheme.shapes.extraLarge,
  content: @Composable (iconSize: Dp) -> Unit,
) {
  val accessoryButtonContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
  val accessoryButtonContentColor = MaterialTheme.colorScheme.onSurface
  val accessoryButtonIconSize = ButtonDefaults.iconSizeFor(max(buttonSize.width, buttonSize.height))
  FilledIconButton(
    onClick = onClick,
    shapes = IconButtonDefaults.shapes(
      shape = buttonShape,
      pressedShape = ButtonDefaults.shape,
    ),
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = accessoryButtonContainerColor,
      contentColor = accessoryButtonContentColor,
    ),
    modifier = modifier
      .sizeIn(
        minWidth = buttonSize.width,
        minHeight = buttonSize.height,
      ),
    content = {
      content(accessoryButtonIconSize)
    },
  )
}

@Composable
private fun PlayPauseButton(
  state: AudioPlayer.State,
  isInteracting: Boolean,
  onClick: () -> Unit,
  extraWidth: Dp,
  modifier: Modifier = Modifier,
  iconSize: Dp = ButtonDefaults.ExtraLargeIconSize,
  buttonSize: Dp = ButtonDefaults.LargeContainerHeight,
  buttonShape: Shape = MaterialTheme.shapes.extraLarge,
) {
  val isPlayPauseEnabled = state != AudioPlayer.State.Finished &&
    state != AudioPlayer.State.Buffering &&
    !isInteracting

  FilledIconButton(
    onClick = onClick,
    enabled = isPlayPauseEnabled,
    shapes = IconButtonDefaults.shapes(
      shape = buttonShape,
      pressedShape = ButtonDefaults.shape,
    ),
    modifier = modifier
      .sizeIn(
        minWidth = buttonSize + extraWidth,
        minHeight = buttonSize,
      ),
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

enum class PlayButtonState {
  Buffering,
  Interacting,
  Playing,
  Paused,
}
