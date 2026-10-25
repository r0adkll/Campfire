// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.campfire.common.compose.toast.LocalToast
import app.campfire.core.toast.Toast
import campfire.infra.updates.impl.generated.resources.Res
import campfire.infra.updates.impl.generated.resources.update_open_build_failed
import org.jetbrains.compose.resources.stringResource

/**
 * Shows what came of [AppUpdates.openBuild]: the [AppUpdates.installSheet] offering the build a
 * build page's Open in App asked for, or a toast when the update source couldn't be asked about
 * it. The signed-in and signed-out windows both host it, inside their theme.
 */
@Composable
fun OpenedBuildSheet(appUpdates: AppUpdates) {
  val openedBuild by appUpdates.openedBuild.collectAsState()
  when (openedBuild) {
    OpenedBuild.Offered -> appUpdates.installSheet.Content(onDismiss = appUpdates::closeOpenedBuild)

    OpenedBuild.Failed -> {
      val toast = LocalToast.current
      val message = stringResource(Res.string.update_open_build_failed)
      LaunchedEffect(Unit) {
        toast.show(message, Toast.Duration.LONG)
        appUpdates.closeOpenedBuild()
      }
    }

    null -> Unit
  }
}
