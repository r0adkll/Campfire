// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.offline

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.campfire.network.reachability.WorkOffline
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.work_offline_back_online
import org.jetbrains.compose.resources.getString

/**
 * Says "Back online" in [hostState] when a stretch of working offline ends on its own, while the
 * UI is showing; nothing is announced when Campfire wasn't on screen to see it.
 */
@Composable
fun BackOnlineSnackbarEffect(
  workOffline: WorkOffline,
  hostState: SnackbarHostState,
) {
  LaunchedEffect(workOffline, hostState) {
    workOffline.sessionEnded.collect {
      hostState.showSnackbar(getString(Res.string.work_offline_back_online))
    }
  }
}
