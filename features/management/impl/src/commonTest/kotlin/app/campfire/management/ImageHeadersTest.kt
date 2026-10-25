// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.management.api.match.ImageSize
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ImageHeadersTest {

  @Test
  fun `a PNG's size is in its header`() {
    assertThat(ImageHeaders.size(png(width = 2400, height = 2400))).isEqualTo(ImageSize(2400, 2400))
  }

  @Test
  fun `a JPEG's size is in its frame, past the segments before it`() {
    val jpeg = bytes(0xFF, 0xD8) +
      // APP0 (JFIF), 16 bytes long
      bytes(0xFF, 0xE0, 0x00, 0x10) + ByteArray(14) +
      // Fill byte, then SOF2 (progressive): length, precision, height 1600, width 1000
      bytes(0xFF, 0xFF, 0xC2, 0x00, 0x11, 0x08, 0x06, 0x40, 0x03, 0xE8) + ByteArray(12)

    assertThat(ImageHeaders.size(jpeg)).isEqualTo(ImageSize(1000, 1600))
  }

  @Test
  fun `a JPEG's Huffman tables aren't its frame`() {
    val jpeg = bytes(0xFF, 0xD8) +
      bytes(0xFF, 0xC4, 0x00, 0x04, 0x00, 0x00) +
      bytes(0xFF, 0xC0, 0x00, 0x11, 0x08, 0x01, 0xF4, 0x01, 0xF4) + ByteArray(12)

    assertThat(ImageHeaders.size(jpeg)).isEqualTo(ImageSize(500, 500))
  }

  @Test
  fun `each kind of WebP keeps its size its own way`() {
    val lossy = webp("VP8 ") { it[26] = 0x20; it[27] = 0x03; it[28] = 0x58; it[29] = 0x02 }
    // 800 wide, 600 tall, each stored less one: 799 = 0x31F, 599 = 0x257
    val lossless = webp("VP8L") {
      it[21] = 0x1F
      it[22] = (0x03 or ((0x257 and 0x03) shl 6)).toByte()
      it[23] = ((0x257 shr 2) and 0xFF).toByte()
      it[24] = ((0x257 shr 10) and 0x0F).toByte()
    }
    val extended = webp("VP8X") { it[24] = 0x1F; it[25] = 0x03; it[27] = 0x57; it[28] = 0x02 }

    assertThat(ImageHeaders.size(lossy)).isEqualTo(ImageSize(800, 600))
    assertThat(ImageHeaders.size(lossless)).isEqualTo(ImageSize(800, 600))
    assertThat(ImageHeaders.size(extended)).isEqualTo(ImageSize(800, 600))
  }

  @Test
  fun `too little of a file, or what isn't an image, has no size yet`() {
    assertThat(ImageHeaders.size(png(2400, 2400).copyOf(20))).isNull()
    assertThat(ImageHeaders.size(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10))).isNull()
    assertThat(ImageHeaders.size("<html>Not found</html>".encodeToByteArray())).isNull()
  }

  private fun png(width: Int, height: Int): ByteArray =
    bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D) +
      "IHDR".encodeToByteArray() + int(width) + int(height) + ByteArray(5)

  private fun webp(chunk: String, fill: (ByteArray) -> Unit): ByteArray {
    val bytes = "RIFF".encodeToByteArray() + ByteArray(4) + "WEBP".encodeToByteArray() +
      chunk.encodeToByteArray() + ByteArray(18)
    return bytes.also(fill)
  }

  private fun int(value: Int) = bytes(value ushr 24, value ushr 16, value ushr 8, value)

  private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
