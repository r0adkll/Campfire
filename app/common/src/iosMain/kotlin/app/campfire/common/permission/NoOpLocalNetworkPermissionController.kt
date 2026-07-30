// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.permission

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.permission.LocalNetworkPermissionController
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/**
 * iOS surfaces its own local-network prompt automatically on first LAN access (driven by
 * `NSLocalNetworkUsageDescription`), so there is nothing to request explicitly here.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpLocalNetworkPermissionController : LocalNetworkPermissionController {
  override suspend fun requestIfNeeded(serverUrl: String): Boolean = true
}
