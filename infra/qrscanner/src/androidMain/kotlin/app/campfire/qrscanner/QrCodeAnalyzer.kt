// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Looks for a QR code in each camera frame's luminance (Y) plane and hands its text to
 * [onDecoded], on the analysis thread. Frames are closed as soon as they're read, never kept.
 */
internal class QrCodeAnalyzer(
  private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {

  private val decoder = QrCodeDecoder()
  private var luminance = ByteArray(0)

  override fun analyze(image: ImageProxy) {
    val text = image.use {
      val plane = it.planes[0]
      val rowStride = plane.rowStride
      val size = rowStride * it.height
      if (luminance.size != size) luminance = ByteArray(size)

      // The last row may stop short of the full stride
      val buffer = plane.buffer
      buffer.rewind()
      buffer.get(luminance, 0, minOf(buffer.remaining(), size))

      decoder.decode(luminance, rowStride, it.width, it.height)
    }
    if (text != null) onDecoded(text)
  }
}
