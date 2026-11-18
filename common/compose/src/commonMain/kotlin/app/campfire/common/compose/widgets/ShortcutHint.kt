// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * The key held with another for a shortcut, as the keyboard labels it: ⌘ on Apple's, Ctrl on the
 * rest. The keys these shortcuts check take either, so this only changes what a hint says.
 */
expect val ShortcutModifierKey: String

/** A key as it's printed on the keyboard, such as ⌘S or Esc */
@Composable
fun KeyCap(
  key: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = MaterialTheme.shapes.extraSmall,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Text(
      text = key,
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
    )
  }
}

/**
 * A shortcut's key beside what it does, such as "⌘S save", for a keyboard's user to learn. Screen
 * readers skip it, as what it does has its own control.
 */
@Composable
fun ShortcutHint(
  key: String,
  action: String,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.clearAndSetSemantics {},
  ) {
    KeyCap(key)
    Text(
      text = action,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

/** [key] held with [ShortcutModifierKey], as it's written: ⌘S, or Ctrl+S */
fun primaryShortcut(key: String): String =
  if (ShortcutModifierKey == "⌘") "⌘$key" else "$ShortcutModifierKey+$key"
