// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.lifecycle

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.lifecycle.AppLifecycleObserver
import app.campfire.core.lifecycle.AppLifecycleState
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

// Desktop reports Foreground unconditionally; window-focus tracking can be added later
// if we want to disconnect when the window is hidden.
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopAppLifecycleObserver : AppLifecycleObserver {
  override val state: StateFlow<AppLifecycleState> =
    MutableStateFlow(AppLifecycleState.Foreground).asStateFlow()
}
