// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The insets the page keeps clear of. They stay the same whether the system bars are showing or
 * not, so showing the reader's bars doesn't lay the book out again.
 */
internal expect val WindowInsets.Companion.readerPage: WindowInsets
  @Composable get
