// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Finds a QR code in a greyscale image, such as the Y plane of a camera frame. QR codes only:
 * other barcode formats aren't looked for. Not thread-safe; use one per analysis thread.
 */
class QrCodeDecoder {

  private val reader = QRCodeReader()

  /**
   * The text of the QR code in [luminance], or `null` when there isn't one that reads cleanly.
   * [luminance] holds [height] rows of [rowStride] bytes, of which the first [width] in each row
   * are the image. A code at any rotation is found.
   */
  fun decode(luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
    val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
    return try {
      reader.decode(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: NotFoundException) {
      null
    } catch (_: ChecksumException) {
      null
    } catch (_: FormatException) {
      null
    } finally {
      reader.reset()
    }
  }
}
