// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.bottombar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.sessions.ui.composables.ForwardIcon
import app.campfire.sessions.ui.composables.RewindIcon
import app.campfire.sessions.ui.playback.expanded.composables.PlayButtonState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_forward
import campfire.features.sessions.ui.generated.resources.action_play_pause
import campfire.features.sessions.ui.generated.resources.action_rewind
import campfire.features.sessions.ui.generated.resources.action_skip_next
import campfire.features.sessions.ui.generated.resources.action_skip_previous
import org.jetbrains.compose.resources.stringResource

/**
 * The desktop transport: the same Material 3 Expressive treatment as the phone player — filled
 * play button that morphs shape on press, tonal accessory buttons at a quarter of primary, skip
 * buttons in the same tone — laid out in one row at desktop proportions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DesktopPlaybackActions(
  state: AudioPlayer.State,
  enabled: Boolean,
  onSkipPreviousClick: () -> Unit,
  onRewindClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = ButtonDefaults.MinHeight,
  playButtonSize: Dp = 44.dp,
  playButtonExtraWidth: Dp = 20.dp,
) {
  val accessoryColors = IconButtonDefaults.filledIconButtonColors(
    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    contentColor = MaterialTheme.colorScheme.onSurface,
  )
  val morphingShapes = IconButtonDefaults.shapes(
    shape = MaterialTheme.shapes.extraLarge,
    pressedShape = ButtonDefaults.shape,
  )
  val iconSize = ButtonDefaults.iconSizeFor(buttonSize)

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
  ) {
    @Composable
    fun Accessory(
      label: String,
      onClick: () -> Unit,
      content: @Composable (iconSize: Dp) -> Unit,
    ) {
      IconButtonTooltip(text = label) {
        FilledIconButton(
          onClick = onClick,
          enabled = enabled,
          shapes = morphingShapes,
          colors = accessoryColors,
          modifier = Modifier.sizeIn(minWidth = buttonSize, minHeight = buttonSize),
        ) {
          content(iconSize)
        }
      }
    }

    @Composable
    fun Skip(label: String, icon: ImageVector, onClick: () -> Unit) {
      IconButtonTooltip(text = label) {
        FilledIconButton(
          onClick = onClick,
          enabled = enabled,
          shapes = IconButtonDefaults.shapes(),
          colors = accessoryColors,
          modifier = Modifier.sizeIn(minWidth = buttonSize, minHeight = buttonSize),
        ) {
          Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
        }
      }
    }

    Skip(stringResource(Res.string.action_skip_previous), CampfireIcons.Rounded.SkipPrevious, onSkipPreviousClick)

    Accessory(stringResource(Res.string.action_rewind), onRewindClick) { size ->
      RewindIcon(modifier = Modifier.size(size))
    }

    val playPauseLabel = stringResource(Res.string.action_play_pause)
    val isPlayPauseEnabled = enabled &&
      state != AudioPlayer.State.Finished &&
      state != AudioPlayer.State.Buffering
    IconButtonTooltip(text = playPauseLabel) {
      FilledIconButton(
        onClick = onPlayPauseClick,
        enabled = isPlayPauseEnabled,
        shapes = morphingShapes,
        modifier = Modifier.sizeIn(
          minWidth = playButtonSize + playButtonExtraWidth,
          minHeight = playButtonSize,
        ),
      ) {
        AnimatedContent(
          targetState = when (state) {
            AudioPlayer.State.Buffering -> PlayButtonState.Buffering
            AudioPlayer.State.Playing -> PlayButtonState.Playing
            else -> PlayButtonState.Paused
          },
          transitionSpec = {
            (fadeIn(initialAlpha = 0.4f) + expandIn(expandFrom = Alignment.Center)) togetherWith
              (fadeOut(targetAlpha = 0.4f) + shrinkOut(shrinkTowards = Alignment.Center))
          },
          contentAlignment = Alignment.Center,
        ) { playState ->
          val playIconSize = ButtonDefaults.iconSizeFor(playButtonSize)
          if (playState == PlayButtonState.Buffering) {
            CircularProgressIndicator(
              modifier = Modifier.size(playIconSize),
              strokeWidth = 3.dp,
            )
          } else {
            val icon = if (playState == PlayButtonState.Playing) {
              CampfireIcons.Rounded.Pause
            } else {
              CampfireIcons.Rounded.PlayArrow
            }
            Icon(
              icon,
              modifier = Modifier.size(playIconSize),
              contentDescription = playPauseLabel,
            )
          }
        }
      }
    }

    Accessory(stringResource(Res.string.action_forward), onForwardClick) { size ->
      ForwardIcon(modifier = Modifier.size(size))
    }

    Skip(stringResource(Res.string.action_skip_next), CampfireIcons.Rounded.SkipNext, onSkipNextClick)
  }
}
