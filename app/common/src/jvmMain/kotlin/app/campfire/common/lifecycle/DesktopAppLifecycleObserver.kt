// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.lifecycle

import app.campfire.core.di.AppScope
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Desktop reports Foreground unconditionally; window-focus tracking can be added later
// if we want to disconnect when the window is hidden.
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopAppLifecycleObserver : AppLifecycleObserver {
  override val state: StateFlow<AppLifecycleState> =
    MutableStateFlow(AppLifecycleState.Foreground).asStateFlow()
}
