// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * For platforms that can't describe their network yet (iOS, desktop): always
 * [NetworkSnapshot.Unknown], so no away-from-home restriction applies there. Replaced on Android.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class UnknownNetworkMonitor : NetworkMonitor {
  override val isSupported: Boolean = false
  override val snapshot: StateFlow<NetworkSnapshot> = MutableStateFlow(NetworkSnapshot.Unknown).asStateFlow()
}
