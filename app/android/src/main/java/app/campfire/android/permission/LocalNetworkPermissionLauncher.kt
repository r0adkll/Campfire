// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.permission

import androidx.activity.result.contract.ActivityResultContracts
import app.campfire.core.ActivityResultFlowLauncher
import app.campfire.core.ComponentActivityPlugin
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Activity-scoped launcher for the runtime `ACCESS_LOCAL_NETWORK` permission request. Registered
 * against [androidx.activity.ComponentActivity] via the [ComponentActivityPlugin] multibinding so
 * `suspend fun launch(permission)` can be called from anywhere (see
 * [AndroidLocalNetworkPermissionController]).
 */
@SingleIn(AppScope::class)
@Inject
@ContributesIntoSet(AppScope::class, binding = binding<ComponentActivityPlugin>())
class LocalNetworkPermissionLauncher : ActivityResultFlowLauncher<String, Boolean>(
  ActivityResultContracts.RequestPermission(),
)
