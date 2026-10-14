// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Reads QR codes with the device's camera, on platforms where Campfire can. Everywhere else the
 * [UnsupportedQrCodeScanner] binding applies, and a platform implementation replaces it.
 */
interface QrCodeScanner {

  /** Whether this device can scan. When it can't, hide anything that leads to [Viewfinder]. */
  val isSupported: Boolean

  /**
   * The camera's live view, filling [modifier]'s bounds, with a guide showing where to hold the
   * code. It asks for camera access itself, and explains what to do while it doesn't have it.
   * [onScanned] gets the text of every QR code it reads, again and again while one stays in view,
   * on the main thread. Frames aren't kept, and scanned text isn't logged.
   *
   * @param captured the caller has what it wanted; the guide shows the code was read
   * @param hint shown just below the guide while the camera is live
   */
  @Composable
  fun Viewfinder(
    onScanned: (String) -> Unit,
    captured: Boolean,
    hint: @Composable () -> Unit,
    modifier: Modifier,
  )
}

/** The [QrCodeScanner] for platforms without one. */
@ContributesBinding(AppScope::class)
@Inject
class UnsupportedQrCodeScanner : QrCodeScanner {
  override val isSupported: Boolean get() = false

  @Composable
  override fun Viewfinder(
    onScanned: (String) -> Unit,
    captured: Boolean,
    hint: @Composable () -> Unit,
    modifier: Modifier,
  ) = Unit
}
