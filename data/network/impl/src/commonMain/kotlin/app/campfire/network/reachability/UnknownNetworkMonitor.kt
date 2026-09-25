// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

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
