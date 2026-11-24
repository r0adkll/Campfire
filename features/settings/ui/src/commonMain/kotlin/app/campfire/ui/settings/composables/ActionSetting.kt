// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

/**
 * A setting row that acts on a tap ([onClick]) and/or a long-press ([onLongClick]). With only a
 * long-press, the row offers no tap action, so a tap neither ripples nor is announced.
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

@Composable
private fun Modifier.longClickOnly(action: () -> Unit, label: String?): Modifier {
  val haptics = LocalHapticFeedback.current
  val currentAction by rememberUpdatedState(action)
  return pointerInput(Unit) {
    detectTapGestures(
      onLongPress = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        currentAction()
      },
    )
  }.semantics {
    onLongClick(label = label) {
      currentAction()
      true
    }
  }
}
