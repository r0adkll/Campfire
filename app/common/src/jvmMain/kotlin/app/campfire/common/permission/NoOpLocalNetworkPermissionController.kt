// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.permission

import app.campfire.core.di.AppScope
import app.campfire.core.permission.LocalNetworkPermissionController
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Desktop has no local-network permission gate, so access is always available. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpLocalNetworkPermissionController : LocalNetworkPermissionController {
  override suspend fun requestIfNeeded(serverUrl: String): Boolean = true
}
