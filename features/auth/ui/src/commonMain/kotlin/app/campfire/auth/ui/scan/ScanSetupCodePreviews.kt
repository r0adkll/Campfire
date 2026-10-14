// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.qrscanner.PreviewQrCodeScanner
import app.campfire.qrscanner.PreviewQrCodeScanner.PreviewCamera

/*
 * Previews of every state the scan screen can be in. The camera's view is a stand-in, but the
 * guide, hints, and camera messages are the real ones.
 */

private const val PhoneWidth = 411
private const val PhoneHeight = 891

@Preview(name = "Waiting for a code", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanWaitingPreview() = ScanSetupCodePreview()

@Preview(name = "Waiting for a code, dark", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanWaitingDarkPreview() = ScanSetupCodePreview(useDarkColors = true)

@Preview(name = "Another QR code", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanOtherCodePreview() = ScanSetupCodePreview(sawOtherCode = true)

@Preview(name = "Setup code read", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanCapturedPreview() = ScanSetupCodePreview(captured = true)

@Preview(name = "Camera permission needed", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanPermissionNeededPreview() = ScanSetupCodePreview(camera = PreviewCamera.PermissionNeeded)

@Preview(name = "Camera permission denied", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanPermissionDeniedPreview() = ScanSetupCodePreview(camera = PreviewCamera.PermissionDenied)

@Preview(name = "Camera unavailable", widthDp = PhoneWidth, heightDp = PhoneHeight)
@Composable
private fun ScanUnavailablePreview() = ScanSetupCodePreview(camera = PreviewCamera.Unavailable)

@Composable
private fun ScanSetupCodePreview(
  camera: PreviewCamera = PreviewCamera.Live,
  sawOtherCode: Boolean = false,
  captured: Boolean = false,
  useDarkColors: Boolean = false,
) {
  CampfireTheme(useDarkColors = useDarkColors) {
    ScanSetupCodeUi(
      state = ScanSetupCodeUiState(sawOtherCode = sawOtherCode, captured = captured, eventSink = {}),
      qrCodeScanner = PreviewQrCodeScanner(camera),
      modifier = Modifier,
    )
  }
}
