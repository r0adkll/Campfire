// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** Scans with CameraX, decoding with ZXing, on any device with a camera. */
@ContributesBinding(AppScope::class, replaces = [UnsupportedQrCodeScanner::class])
@Inject
class CameraQrCodeScanner(
  private val application: Application,
) : QrCodeScanner {

  override val isSupported: Boolean
    get() = application.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

  @Composable
  override fun Viewfinder(onScanned: (String) -> Unit, modifier: Modifier) {
    CameraViewfinder(onScanned = onScanned, modifier = modifier)
  }
}
