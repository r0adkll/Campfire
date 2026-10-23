// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.offline.test

import app.campfire.offline.DownloadConditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Downloads may fetch while [allowed] is true, as it is to start with. */
class FakeDownloadConditions : DownloadConditions {

  val allowed = MutableStateFlow(true)

  override fun observeAllowed(): Flow<Boolean> = allowed
}
