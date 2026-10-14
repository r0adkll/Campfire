// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.qrscanner.PreviewQrCodeScanner.PreviewCamera

/*
 * The viewfinder on its own, in each state it can show. The scan screen's previews show it with
 * the screen's top bar and hints.
 */

private const val PhoneWidth = 411
private const val PhoneHeight = 891

@Preview(name = "Waiting for a code", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ViewfinderWaitingPreview() = ViewfinderPreview(PreviewCamera.Live)

@Preview(name = "Code read", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ViewfinderCapturedPreview() = ViewfinderPreview(PreviewCamera.Live, captured = true)

@Preview(name = "Camera permission needed", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ViewfinderPermissionNeededPreview() = ViewfinderPreview(PreviewCamera.PermissionNeeded)

@Preview(name = "Camera permission denied", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ViewfinderPermissionDeniedPreview() = ViewfinderPreview(PreviewCamera.PermissionDenied)

@Preview(name = "Camera unavailable", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ViewfinderUnavailablePreview() = ViewfinderPreview(PreviewCamera.Unavailable)

@Composable
private fun ViewfinderPreview(
  camera: PreviewCamera,
  captured: Boolean = false,
) {
  CampfireTheme {
    Box(
      Modifier
        .fillMaxSize()
        .background(Color.Black),
    ) {
      PreviewQrCodeScanner(camera).Viewfinder(
        onScanned = {},
        captured = captured,
        hint = {
          Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.large) {
            Text("Hint", Modifier.padding(16.dp))
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
