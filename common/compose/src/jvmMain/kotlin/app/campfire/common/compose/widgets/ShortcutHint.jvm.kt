// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

actual val ShortcutModifierKey: String =
  if (System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)) "⌘" else "Ctrl"
