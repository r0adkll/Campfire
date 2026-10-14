// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.qrscanner

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.test.Test

class QrCodeDecoderTest {

  private val decoder = QrCodeDecoder()

  @Test
  fun reads_a_setup_link_sized_qr_code() {
    val frame = Frame.of(qrCode(SETUP_LINK))

    assertThat(decoder.decode(frame.bytes, frame.rowStride, frame.width, frame.height)).isEqualTo(SETUP_LINK)
  }

  @Test
  fun reads_a_frame_whose_rows_are_padded() {
    // Camera planes often have a row stride wider than the image
    val frame = Frame.of(qrCode(SETUP_LINK), padding = 37)

    assertThat(decoder.decode(frame.bytes, frame.rowStride, frame.width, frame.height)).isEqualTo(SETUP_LINK)
  }

  @Test
  fun reads_a_code_turned_sideways() {
    // A portrait phone hands over landscape sensor frames
    val frame = Frame.of(qrCode(SETUP_LINK).rotated())

    assertThat(decoder.decode(frame.bytes, frame.rowStride, frame.width, frame.height)).isEqualTo(SETUP_LINK)
  }

  @Test
  fun a_frame_without_a_qr_code_reads_nothing() {
    val blank = Frame(ByteArray(320 * 240) { 0xC0.toByte() }, rowStride = 320, width = 320, height = 240)

    assertThat(decoder.decode(blank.bytes, blank.rowStride, blank.width, blank.height)).isNull()
  }

  @Test
  fun other_barcode_formats_are_ignored() {
    val dataMatrix = DataMatrixWriter().encode("not a qr code", BarcodeFormat.DATA_MATRIX, 240, 240)
    val frame = Frame.of(dataMatrix)

    assertThat(decoder.decode(frame.bytes, frame.rowStride, frame.width, frame.height)).isNull()
  }

  @Test
  fun one_decoder_reads_frame_after_frame() {
    val first = Frame.of(qrCode("first"))
    val second = Frame.of(qrCode("second"))

    assertThat(decoder.decode(first.bytes, first.rowStride, first.width, first.height)).isEqualTo("first")
    assertThat(decoder.decode(second.bytes, second.rowStride, second.width, second.height)).isEqualTo("second")
  }

  private fun qrCode(text: String): BitMatrix = QRCodeWriter().encode(
    text,
    BarcodeFormat.QR_CODE,
    480,
    480,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4),
  )

  private fun BitMatrix.rotated(): BitMatrix {
    val rotated = BitMatrix(height, width)
    for (y in 0 until height) {
      for (x in 0 until width) {
        if (get(x, y)) rotated.set(height - 1 - y, x)
      }
    }
    return rotated
  }

  /** A greyscale frame: [height] rows of [rowStride] bytes, the first [width] of each the image. */
  private class Frame(val bytes: ByteArray, val rowStride: Int, val width: Int, val height: Int) {
    companion object {
      fun of(matrix: BitMatrix, padding: Int = 0): Frame {
        val rowStride = matrix.width + padding
        val bytes = ByteArray(rowStride * matrix.height)
        for (y in 0 until matrix.height) {
          for (x in 0 until rowStride) {
            val dark = x < matrix.width && matrix.get(x, y)
            bytes[y * rowStride + x] = if (dark) 0x10 else 0xF0.toByte()
          }
        }
        return Frame(bytes, rowStride, matrix.width, matrix.height)
      }
    }
  }

  private companion object {
    // The size of a real one: a ~40-character Client ID and a 64-character secret
    const val SETUP_LINK = "https://thescavengers.software/campfire/connect#v=1" +
      "&server=https%3A%2F%2Fabs.firenation.app&name=Fire+Nation&auth=openid" +
      "&id=0123456789abcdef0123456789abcdef.access" +
      "&secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
  }
}
