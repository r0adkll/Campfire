// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.core.model.Session
import app.campfire.sessions.ui.playback.PlayerUiEvent
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackActions
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackSeekBar
import app.campfire.sessions.ui.playback.expanded.composables.SmallThumbSize

/**
 * The cover, cropped to fill the whole player and tinted with the player's own container colour,
 * for the compact layout where the art becomes the backdrop rather than a framed image.
 */
@Composable
internal fun CoverBackdrop(
  imageUrl: String?,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier) {
    CoverImage(
      imageUrl = imageUrl,
      contentDescription = null,
      size = Dp.Unspecified,
      requestSize = maxOf(maxWidth, maxHeight),
      shape = RectangleShape,
      contentScale = ContentScale.Crop,
      sharedElementModifier = Modifier.fillMaxSize(),
      modifier = Modifier.fillMaxSize(),
    )
    Box(
      Modifier
        .fillMaxSize()
        .background(tint.copy(alpha = BackdropTintAlpha)),
    )
  }
}

/**
 * The player body for a small, roughly square window: the titles, the seek bar and the transport
 * over the [CoverBackdrop], with no tool row — there is no room for one, and the main window's
 * bar still carries every tool. Tapping the titles opens the item, as the cover does elsewhere.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CompactDedicatedPlaybackContent(
  session: Session,
  playerState: PlayerUiState,
  onItemClick: (Session) -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isDragged by interactionSource.collectIsDraggedAsState()
  val isInteracting = isPressed || isDragged

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onItemClick(session) }
        .padding(
          horizontal = 24.dp,
          vertical = 4.dp,
        ),
    ) {
      Text(
        text = playerState.metadata.title ?: session.title,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PaytoneOneFontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = session.libraryItem.media.metadata.title ?: "",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.alpha(0.8f),
      )
    }

    Spacer(Modifier.height(8.dp))

    PlaybackSeekBar(
      state = playerState.state,
      currentTime = playerState.time,
      currentDuration = playerState.duration,
      playbackSpeed = playerState.speed,
      onSeek = { percent ->
        playerState.eventSink(PlayerUiEvent.Seek.Percent(percent))
      },
      interactionSource = interactionSource,
      waveEnabled = playerState.wavySliderEnabled,
      thumbSize = SmallThumbSize,
    )

    Spacer(Modifier.height(12.dp))

    PlaybackActions(
      state = playerState.state,
      isInteracting = isInteracting,
      buttonSize = CompactTransportButtonSize,
      buttonShape = MaterialTheme.shapes.largeIncreased,
      playButtonExtraWidth = 64.dp,
      onSkipPreviousClick = { playerState.eventSink(PlayerUiEvent.PreviousClick) },
      onRewindClick = { playerState.eventSink(PlayerUiEvent.RewindClick) },
      onPlayPauseClick = { playerState.eventSink(PlayerUiEvent.PlayPauseClick) },
      onForwardClick = { playerState.eventSink(PlayerUiEvent.FastForwardClick) },
      onSkipNextClick = { playerState.eventSink(PlayerUiEvent.NextClick) },
    )

    Spacer(Modifier.height(16.dp))
  }
}

/** Heavy enough that text and controls stay legible over any cover, light enough to read as art. */
private const val BackdropTintAlpha = 0.78f

private val CompactTransportButtonSize = 80.dp
