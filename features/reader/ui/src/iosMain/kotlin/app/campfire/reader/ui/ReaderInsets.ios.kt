// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

// The safe area doesn't change when the status bar and home indicator hide
internal actual val WindowInsets.Companion.readerPage: WindowInsets
  @Composable get() = safeDrawing
