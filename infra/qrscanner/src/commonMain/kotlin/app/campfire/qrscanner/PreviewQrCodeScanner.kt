// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A [QrCodeScanner] for previews and render tests. It draws the real guide and messages, over a
 * stand-in for the camera's view, in the state [camera] names. It never reads anything.
 */
class PreviewQrCodeScanner(
  private val camera: PreviewCamera = PreviewCamera.Live,
) : QrCodeScanner {

  enum class PreviewCamera {
    /** The camera is showing its view. */
    Live,

    /** Campfire may still ask for the camera. */
    PermissionNeeded,

    /** Android won't ask for the camera again; only app settings can allow it. */
    PermissionDenied,

    /** Campfire may use the camera, but couldn't open it. */
    Unavailable,
  }

  override val isSupported: Boolean get() = true

  @Composable
  override fun Viewfinder(
    onScanned: (String) -> Unit,
    captured: Boolean,
    hint: @Composable () -> Unit,
    modifier: Modifier,
  ) {
    when (camera) {
      PreviewCamera.Live -> Box(modifier.background(StandInView)) {
        ViewfinderGuide(captured = captured, hint = hint, modifier = Modifier.fillMaxSize())
      }

      PreviewCamera.PermissionNeeded -> CameraPermissionMessage(denied = false, onAction = {}, modifier = modifier)
      PreviewCamera.PermissionDenied -> CameraPermissionMessage(denied = true, onAction = {}, modifier = modifier)
      PreviewCamera.Unavailable -> CameraUnavailableMessage(modifier)
    }
  }
}

private val StandInView = Brush.linearGradient(
  listOf(Color(0xFF6E7F63), Color(0xFFC9B79C), Color(0xFF8A6F5A), Color(0xFF3E4A57)),
)
