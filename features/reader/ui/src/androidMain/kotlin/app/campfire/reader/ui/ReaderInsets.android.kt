// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable

internal actual val WindowInsets.Companion.readerPage: WindowInsets
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  get() = systemBarsIgnoringVisibility.union(displayCutout)
