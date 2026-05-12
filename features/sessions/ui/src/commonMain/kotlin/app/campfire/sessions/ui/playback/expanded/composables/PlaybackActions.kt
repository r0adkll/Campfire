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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.sessions.ui.composables.ForwardIcon
import app.campfire.sessions.ui.composables.RewindIcon
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_forward
import campfire.features.sessions.ui.generated.resources.action_play_pause
import campfire.features.sessions.ui.generated.resources.action_rewind
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
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val playButtonExtraWidth = 32.dp
    val playButtonSize = ButtonDefaults.LargeContainerHeight
    val accessoryButtonContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val accessoryButtonContentColor = MaterialTheme.colorScheme.onSurface

    Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
      val buttonShape = MaterialTheme.shapes.extraLarge

      @Composable
      fun AccessoryButton(
        onClick: () -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        content: @Composable (iconSize: Dp) -> Unit,
      ) {
        val accessoryButtonSize = ButtonDefaults.LargeContainerHeight
        val accessoryButtonIconSize = ButtonDefaults.LargeIconSize
        IconButtonTooltip(text = label, modifier = modifier) {
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
            modifier = Modifier
              .sizeIn(
                minWidth = accessoryButtonSize,
                minHeight = accessoryButtonSize,
              ),
            content = {
              content(accessoryButtonIconSize)
            },
          )
        }
      }

      AccessoryButton(
        onClick = onRewindClick,
        label = stringResource(Res.string.action_rewind),
      ) { iconSize ->
        RewindIcon(
          modifier = Modifier.size(iconSize),
        )
      }

      val isPlayPauseEnabled = state != AudioPlayer.State.Finished &&
        state != AudioPlayer.State.Buffering &&
        !isInteracting

      val playButtonIconSize = ButtonDefaults.ExtraLargeIconSize
      val playPauseLabel = stringResource(Res.string.action_play_pause)
      IconButtonTooltip(text = playPauseLabel) {
        FilledIconButton(
          onClick = onPlayPauseClick,
          enabled = isPlayPauseEnabled,
          shapes = IconButtonDefaults.shapes(
            shape = buttonShape,
            pressedShape = ButtonDefaults.shape,
          ),
          modifier = Modifier
            .sizeIn(
              minWidth = playButtonSize + playButtonExtraWidth,
              minHeight = playButtonSize,
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
                modifier = Modifier.size(playButtonIconSize),
                strokeWidth = 4.dp,
              )
            } else {
              Icon(
                when (state) {
                  PlayButtonState.Interacting -> Icons.Rounded.EditAudio
                  PlayButtonState.Playing -> Icons.Rounded.Pause
                  else -> Icons.Rounded.PlayArrow
                },
                modifier = Modifier.size(playButtonIconSize),
                contentDescription = playPauseLabel,
              )
            }
          }
        }
      }

      AccessoryButton(
        onClick = onForwardClick,
        label = stringResource(Res.string.action_forward),
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
          max = (ButtonDefaults.LargeContainerHeight * 3) + playButtonExtraWidth + 8.dp,
        ),
    ) {
      val buttonSize = ButtonDefaults.MinHeight
      val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = accessoryButtonContainerColor,
        contentColor = accessoryButtonContentColor,
      )
      val shapes = IconButtonDefaults.shapes()

      val skipPreviousLabel = stringResource(Res.string.action_skip_previous)
      IconButtonTooltip(
        text = skipPreviousLabel,
        modifier = Modifier.weight(1f),
      ) {
        FilledIconButton(
          onClick = onSkipPreviousClick,
          shapes = shapes,
          colors = colors,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(buttonSize),
        ) {
          Icon(
            Icons.Rounded.SkipPrevious,
            modifier = Modifier.size(ButtonDefaults.LargeIconSize),
            contentDescription = skipPreviousLabel,
          )
        }
      }
      val skipNextLabel = stringResource(Res.string.action_skip_next)
      IconButtonTooltip(
        text = skipNextLabel,
        modifier = Modifier.weight(1f),
      ) {
        FilledIconButton(
          onClick = onSkipNextClick,
          shapes = shapes,
          colors = colors,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(buttonSize),
        ) {
          Icon(
            Icons.Rounded.SkipNext,
            modifier = Modifier.size(ButtonDefaults.LargeIconSize),
            contentDescription = skipNextLabel,
          )
        }
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
