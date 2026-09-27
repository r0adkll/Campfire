// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import app.campfire.audioplayer.cast.CastController
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastState
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Default Android [CastController] for builds without the optional
 * `:infra:audioplayer:cast` module (e.g. the foss flavor): casting is permanently
 * unavailable and the cast UI hides itself. Replaced by `MediaRouterCastController`
 * when the cast module is present.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpCastController : CastController {
  override val state = MutableStateFlow(CastState.Unavailable)
  override val availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
  override fun connect(device: CastDevice) {
  }
}
