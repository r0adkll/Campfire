// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Window insets for [TopAppBar]s that might be displayed in a context where
 * the supporting content layout is visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
val CampfireTopAppBarInsets: WindowInsets
  @Composable get() {
    val chromeInsets = LocalWindowChromeInsets.current

    // Union rather than add, so a cutout that overlaps the system bars is only counted once.
    return TopAppBarDefaults.windowInsets
      .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
      .add(chromeInsets)
  }
