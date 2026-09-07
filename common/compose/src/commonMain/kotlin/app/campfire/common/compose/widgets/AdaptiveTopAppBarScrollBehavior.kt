// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import app.campfire.core.forPlatform

/**
 * [TopAppBarDefaults.enterAlwaysScrollBehavior] on touch platforms, pinned on desktop.
 *
 * A mouse wheel produces no fling, so a collapsing bar settles wherever the last notch left it —
 * usually half off-screen, taking the navigation and action icons with it.
 */
@Composable
fun adaptiveEnterAlwaysScrollBehavior(): TopAppBarScrollBehavior = forPlatform(
  mobile = { TopAppBarDefaults.enterAlwaysScrollBehavior() },
  desktop = { TopAppBarDefaults.pinnedScrollBehavior() },
)

/**
 * [TopAppBarDefaults.exitUntilCollapsedScrollBehavior] on touch platforms, pinned on desktop.
 * @see adaptiveEnterAlwaysScrollBehavior
 */
@Composable
fun adaptiveExitUntilCollapsedScrollBehavior(): TopAppBarScrollBehavior = forPlatform(
  mobile = { TopAppBarDefaults.exitUntilCollapsedScrollBehavior() },
  desktop = { TopAppBarDefaults.pinnedScrollBehavior() },
)
