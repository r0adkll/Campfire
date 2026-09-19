// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.sessions.ui.composables.OutputDeviceControl
import app.campfire.sessions.ui.composables.VolumeControl
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.VolumeUiState
import app.campfire.sessions.ui.playback.expanded.ItemActions
import app.campfire.sessions.ui.playback.expanded.ItemMetadata
import app.campfire.sessions.ui.playback.expanded.SmallTransportButtonSize
import app.campfire.sessions.ui.playback.expanded.composables.ActionColumn
import app.campfire.sessions.ui.playback.expanded.rememberPlaybackOptionActions
import com.slack.circuit.overlay.OverlayHost

/**
 * The player body for a wide, short region — a foldable's lower half, or a mini-player window
 * dragged wider than it is tall. It is the landscape-phone arrangement: cover and titles on the
 * left, transport and seek bar in the middle, tools down the right edge, with a transport that
 * grows with the room available.
 *
 * Such a region runs from roughly 340dp tall (a Pixel Fold's half) upwards, so the transport size
 * is derived from the measured height rather than fixed: everything else in the middle column has
 * a known height, and whatever is left over — within the small and large transport sizes — goes to
 * the buttons. Should a region ever be too short for even the small transport with the book-time
 * readout, the readout gives way rather than the buttons.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SharedTransitionScope.WideDedicatedPlaybackContent(
  overlayHost: OverlayHost,

  session: Session?,
  playerState: PlayerUiState,
  syncState: SyncUiState,
  itemValidation: LibraryItemValidation,
  playbackHistoryEnabled: Boolean,
  volumeState: VolumeUiState?,
  outputDeviceState: OutputDeviceUiState?,

  onItemClick: (Session) -> Unit,

  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isDragged by interactionSource.collectIsDraggedAsState()
  val isInteracting = isPressed || isDragged

  BoxWithConstraints(modifier) {
    val room = maxHeight - ControlsColumnFixedHeight
    val showBookTime = room >= SmallTransportButtonSize
    val transportSize = (if (showBookTime) room else room + BookTimeReadoutHeight)
      .coerceIn(SmallTransportButtonSize, ButtonDefaults.LargeContainerHeight)

    // Each column centres its content, so they all get the same vertical margin: the cover
    // column and the tool column already end 16dp short of the bottom, and this tops the
    // three of them up to match. Without it the body sits flush under the rail's first
    // button and reads as pushed upwards.
    Row(
      Modifier
        .fillMaxSize()
        .padding(top = WideVerticalMargin),
    ) {
      this@WideDedicatedPlaybackContent.ItemMetadata(
        playerState = playerState,
        session = session,
        itemValidation = itemValidation,
        animatedVisibilityScope = animatedVisibilityScope,
        onItemClick = onItemClick,
        showBookTime = showBookTime,
        modifier = Modifier
          .fillMaxHeight()
          .weight(0.8f),
      )

      ItemActions(
        session = session,
        playerState = playerState,
        syncState = syncState,
        isInteracting = isInteracting,
        interactionSource = interactionSource,
        buttonSize = transportSize,
        modifier = Modifier
          .fillMaxHeight()
          .weight(1.2f)
          .padding(bottom = WideVerticalMargin),
      )

      val actions = rememberPlaybackOptionActions(
        overlayHost = overlayHost,
        session = session,
        playerState = playerState,
      )
      ActionColumn(
        onBookmarksClick = actions.onBookmarksClick,
        speedContent = actions.speedContent,
        timerContent = actions.timerContent,
        volumeContent = volumeState?.let { { VolumeControl(state = it) } },
        outputDeviceContent = outputDeviceState?.let { { OutputDeviceControl(state = it) } },
        onEqualizerClick = actions.onEqualizerClick,
        showEqualizer = playerState.equalizer !is EqualizerState.Unsupported,
        onChapterListClick = actions.onChapterListClick,
        showChapters = session?.episodeId == null,
        onDescriptionClick = actions.onDescriptionClick,
        showDescription = session?.episodeId != null,
        onHistoryClick = actions.onHistoryClick,
        showHistory = playbackHistoryEnabled,
        modifier = Modifier.padding(bottom = WideVerticalMargin),
      )
    }
  }
}

/** The space above and below every column, matching the cover column's own trailing gap. */
private val WideVerticalMargin: Dp = 16.dp

/**
 * Everything in the middle column apart from the main transport row: the skip row and its gap,
 * the book-time readout, the seek bar with its labels, the vertical margins, and a little
 * breathing room.
 */
private val ControlsColumnFixedHeight: Dp = 200.dp

/** The book-time readout, its progress bar and their spacing — reclaimed on the tightest halves. */
private val BookTimeReadoutHeight: Dp = 28.dp
