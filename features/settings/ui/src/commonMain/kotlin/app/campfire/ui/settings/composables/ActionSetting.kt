// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.composables

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

/**
 * A setting row that acts on a tap ([onClick]) and/or a long-press ([onLongClick]). With only a
 * long-press, the row offers no tap action to accessibility services or a tap.
 */
@Composable
internal fun ActionSetting(
  headlineContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  supportingContent: (@Composable () -> Unit)? = null,
  leadingContent: (@Composable () -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
  onLongClickLabel: String? = null,
) {
  val clickModifier = when {
    onClick != null -> Modifier.combinedClickable(
      onClick = onClick,
      onLongClick = onLongClick,
      onLongClickLabel = onLongClickLabel,
    )
    onLongClick != null -> Modifier.longClickOnly(onLongClick, onLongClickLabel)
    else -> Modifier.clickable(enabled = false, onClick = {})
  }

  SettingListItem(
    leadingContent = leadingContent,
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    trailingContent = trailingContent,
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = modifier.then(clickModifier),
  )
}

/**
 * A long-press with no tap action: the row ripples once held past the long-press timeout (a quick
 * tap shows nothing) and can take keyboard focus, where Enter or Space triggers it, since a key has
 * no long-press of its own.
 */
@Composable
private fun Modifier.longClickOnly(action: () -> Unit, label: String?): Modifier {
  val haptics = LocalHapticFeedback.current
  val currentAction by rememberUpdatedState(action)
  val interactionSource = remember { MutableInteractionSource() }
  return indication(interactionSource, LocalIndication.current)
    .focusable(interactionSource = interactionSource)
    .onKeyEvent { event ->
      val activates = event.type == KeyEventType.KeyUp && event.key in ActivationKeys
      if (activates) currentAction()
      activates
    }
    .pointerInput(Unit) {
      awaitEachGesture {
        val down = awaitFirstDown()
        // Non-null when the pointer lifts (or a scroll takes it) before the long-press timeout.
        val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
          waitForUpOrCancellation()
          true
        }
        if (endedEarly != null) return@awaitEachGesture

        // Held long enough: only now ripple, so a quick tap shows nothing.
        val press = PressInteraction.Press(down.position)
        interactionSource.tryEmit(press)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentAction()
        val up = waitForUpOrCancellation()
        interactionSource.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
      }
    }
    .semantics {
      onLongClick(label = label) {
        currentAction()
        true
      }
    }
}

private val ActivationKeys = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)
