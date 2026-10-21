// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.extensions.asReadableBytes
import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateProgress
import app.campfire.updates.source.AppUpdateSheet
import app.campfire.updates.source.AppUpdateSource
import campfire.infra.updates.impl.generated.resources.Res
import campfire.infra.updates.impl.generated.resources.update_sheet_canceled
import campfire.infra.updates.impl.generated.resources.update_sheet_downloaded
import campfire.infra.updates.impl.generated.resources.update_sheet_downloading
import campfire.infra.updates.impl.generated.resources.update_sheet_failed
import campfire.infra.updates.impl.generated.resources.update_sheet_queued
import campfire.infra.updates.impl.generated.resources.update_sheet_title
import campfire.infra.updates.impl.generated.resources.update_sheet_try_again
import campfire.infra.updates.impl.generated.resources.update_sheet_update_now
import campfire.infra.updates.impl.generated.resources.update_sheet_version
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Campfire's own update sheet, for sources without one of their own: shows the waiting [update]'s
 * release notes and drives [AppUpdateSource.installUpdate], reporting its progress.
 */
internal class CampfireAppUpdateSheet(
  private val appUpdateSource: AppUpdateSource,
  private val update: StateFlow<AppUpdate?>,
) : AppUpdateSheet {

  @Composable
  override fun Content(onDismiss: () -> Unit) {
    val overlayHost = LocalOverlayHost.current
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(Unit) {
      val appUpdate = update.value
      if (appUpdate != null) {
        overlayHost.show(
          BottomSheetOverlay<AppUpdate, Unit>(
            model = appUpdate,
            sheetShape = RoundedCornerShape(
              topStart = 32.dp,
              topEnd = 32.dp,
            ),
            onDismiss = { },
            skipPartiallyExpandedState = true,
          ) { model, _ ->
            AppUpdateSheetContent(model)
          },
        )
      }
      dismiss()
    }
  }

  @Composable
  private fun AppUpdateSheetContent(
    appUpdate: AppUpdate,
    modifier: Modifier = Modifier,
  ) {
    val scope = rememberCoroutineScope()
    var updateProgress by remember { mutableStateOf<AppUpdateProgress?>(null) }

    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = stringResource(Res.string.update_sheet_title),
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = PaytoneOneFontFamily,
      )

      Text(
        text = stringResource(Res.string.update_sheet_version, appUpdate.versionName, appUpdate.versionCode),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      val releaseNotes = appUpdate.releaseNotes?.trim()
      if (!releaseNotes.isNullOrBlank()) {
        Spacer(Modifier.height(16.dp))
        Text(
          text = releaseNotes,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
        )
      }

      Spacer(Modifier.height(24.dp))

      val progress = updateProgress
      if (progress == null || progress.status.isTerminal) {
        if (progress != null) {
          Text(
            text = progress.statusText(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
          Spacer(Modifier.height(8.dp))
        }
        Button(
          onClick = {
            scope.launch {
              appUpdateSource.installUpdate()
                .collect { updateProgress = it }
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            stringResource(
              if (progress == null) Res.string.update_sheet_update_now else Res.string.update_sheet_try_again,
            ),
          )
        }
      } else {
        if (progress.status == AppUpdateProgress.Status.Pending) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          LinearProgressIndicator(
            progress = { progress.progress },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        Spacer(Modifier.height(8.dp))
        Text(
          text = progress.statusText(),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}

private val AppUpdateProgress.Status.isTerminal: Boolean
  get() = this == AppUpdateProgress.Status.Failed || this == AppUpdateProgress.Status.Canceled

@Composable
private fun AppUpdateProgress.statusText(): String = when (status) {
  AppUpdateProgress.Status.Pending -> stringResource(Res.string.update_sheet_queued)
  AppUpdateProgress.Status.Downloading -> stringResource(
    Res.string.update_sheet_downloading,
    bytes.asReadableBytes(),
    totalBytes.asReadableBytes(),
  )
  AppUpdateProgress.Status.Downloaded -> stringResource(Res.string.update_sheet_downloaded)
  AppUpdateProgress.Status.Failed -> stringResource(Res.string.update_sheet_failed)
  AppUpdateProgress.Status.Canceled -> stringResource(Res.string.update_sheet_canceled)
}
