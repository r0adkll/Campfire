// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.composables

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.VolumeDown
import app.campfire.common.compose.icons.rounded.VolumeOff
import app.campfire.common.compose.icons.rounded.VolumeUp
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.sessions.ui.playback.VolumeUiEvent
import app.campfire.sessions.ui.playback.VolumeUiState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_mute
import campfire.features.sessions.ui.generated.resources.action_unmute
import campfire.features.sessions.ui.generated.resources.action_volume
import campfire.features.sessions.ui.generated.resources.volume_content_description
import org.jetbrains.compose.resources.stringResource

/**
 * The app's own volume — an icon in the playback bar that opens a vertical slider and a mute
 * toggle. Only rendered where an app-level volume exists at all; see
 * [app.campfire.audioplayer.AudioOutputController].
 *
 * The slider lives in a popup rather than inline because the docked desktop bar appears from the
 * Expanded breakpoint (840dp) up, and at that width the action side has room for roughly a third
 * of what an inline slider needs — it was measured being crushed to a 40dp sliver. One icon fits
 * at every width, on every surface that shows a playback bar.
 *
 * The slider carries the user's chosen *position*, not the gain: the perceptual taper that turns
 * one into the other lives with the player, so this stays a plain 0..1 slider.
 *
 * Unlike its neighbours in the playback bar this has no `enabled` flag — volume belongs to the
 * app rather than to whatever is loaded, so it stays usable with nothing playing.
 */
@Composable
internal fun VolumeControl(
  state: VolumeUiState,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val volumeLabel = stringResource(Res.string.action_volume)

  Box(modifier = modifier) {
    IconButtonTooltip(text = volumeLabel) {
      IconButton(onClick = { expanded = true }) {
        Icon(state.icon, contentDescription = volumeLabel)
      }
    }

    // Held in composition until the exit animation finishes, so closing plays out instead of
    // the popup blinking away.
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded

    if (transitionState.currentState || transitionState.targetState) {
      // A raw Popup rather than DropdownMenu: the menu's position provider clamps the popup to
      // MenuVerticalMargin (an internal 48.dp) from the window edges *after* applying `offset`,
      // and the playback bar sits inside that margin — so no offset can make a menu reach back
      // over its own anchor. See OverAnchorPositionProvider.
      Popup(
        popupPositionProvider = OverAnchorPositionProvider,
        onDismissRequest = { expanded = false },
        properties = PopupProperties(focusable = true),
      ) {
        VolumePopupSurface(transitionState) {
          VolumeSliderColumn(state)
        }
      }
    }
  }
}

/**
 * The popup's container, scaling and fading out of the button that opened it.
 *
 * Scale and alpha ride one transition rather than two `animateFloatAsState` calls so they cannot
 * drift apart, and both are read inside the [graphicsLayer] block — they change every frame, and
 * reading them there keeps the work in the draw phase instead of recomposing the surface.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VolumePopupSurface(
  transitionState: MutableTransitionState<Boolean>,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val transition = rememberTransition(transitionState, label = "VolumePopup")
  val scale by transition.animateFloat(
    transitionSpec = { MaterialTheme.motionScheme.fastSpatialSpec() },
    label = "scale",
  ) { visible -> if (visible) 1f else ClosedScale }
  val alpha by transition.animateFloat(
    transitionSpec = { MaterialTheme.motionScheme.fastEffectsSpec() },
    label = "alpha",
  ) { visible -> if (visible) 1f else 0f }

  Surface(
    modifier = modifier.graphicsLayer {
      scaleX = scale
      scaleY = scale
      this.alpha = alpha
      // Grow from the mute button at the foot of the column, which is the one sitting over the
      // anchor — measured off the live height so it stays right whatever the slider length is.
      transformOrigin = TransformOrigin(
        pivotFractionX = 0.5f,
        pivotFractionY = 1f - (MuteButtonSize.toPx() / 2f / size.height),
      )
    },
    shape = RoundedCornerShape(PopupCornerRadius),
    color = MenuDefaults.containerColor,
    tonalElevation = MenuDefaults.TonalElevation,
    shadowElevation = MenuDefaults.ShadowElevation,
    content = content,
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VolumeSliderColumn(
  state: VolumeUiState,
  modifier: Modifier = Modifier,
) {
  val muteLabel = stringResource(if (state.isMuted) Res.string.action_unmute else Res.string.action_mute)
  val sliderLabel = stringResource(Res.string.volume_content_description)

  // Controlled, exactly as the value-based Slider overload drives its own state: the slider
  // reports a drag and the volume comes back round through the controller, never from here.
  val sliderState = remember { SliderState() }
  sliderState.onValueChange = { state.eventSink(VolumeUiEvent.SetVolume(it)) }
  sliderState.value = state.volume

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Spacer(Modifier.height(16.dp))

    val interactionSource = remember { MutableInteractionSource() }
    VerticalSlider(
      state = sliderState,
      // Louder is up; the default runs the other way
      reverseDirection = true,
      interactionSource = interactionSource,
      thumb = {
        SliderDefaults.Thumb(
          interactionSource = interactionSource,
          sliderState = sliderState,
          thumbSize = DpSize(32.dp, 4.dp),
        )
      },
      modifier = Modifier
        .height(SliderHeight)
        .semantics { contentDescription = sliderLabel },
    )

    IconButtonTooltip(text = muteLabel) {
      IconButton(onClick = { state.eventSink(VolumeUiEvent.ToggleMute) }) {
        Icon(state.icon, contentDescription = muteLabel)
      }
    }
  }
}

/**
 * Lands the popup's bottom edge exactly on the anchor's, centred over it, so the mute button at
 * the foot of the column covers the icon that opened the popup — the icon stays put and the
 * slider grows upward out of it.
 *
 * Deliberately unclamped vertically: overlapping the anchor is the whole point, and every
 * built-in menu provider treats that as something to correct.
 */
private object OverAnchorPositionProvider : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
    val y = anchorBounds.bottom - popupContentSize.height
    return IntOffset(
      x = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
      y = y.coerceAtLeast(0),
    )
  }
}

/**
 * Muted always reads as off, however far up the slider is — otherwise the icon would contradict
 * the silence. Below the threshold the quieter glyph shows the slider is doing something.
 */
private val VolumeUiState.icon: ImageVector
  get() = when {
    isMuted || volume <= 0f -> CampfireIcons.Rounded.VolumeOff
    volume < QuietVolumeThreshold -> CampfireIcons.Rounded.VolumeDown
    else -> CampfireIcons.Rounded.VolumeUp
  }

private const val QuietVolumeThreshold = 0.5f
private const val ClosedScale = 0.8f
private val MuteButtonSize = 48.dp
private val SliderHeight = 140.dp
private val PopupCornerRadius = 24.dp
