// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.management.api.match.ImageSize

/** Reads an image's size from the start of its file: JPEG, PNG and WebP, the formats covers come in. */
internal object ImageHeaders {

  /** The size [bytes] say their image is, or null when they're not enough yet or not an image known here */
  fun size(bytes: ByteArray): ImageSize? = when {
    bytes.startsWith(PngSignature) -> png(bytes)
    bytes.size >= 2 && bytes.u8(0) == 0xFF && bytes.u8(1) == 0xD8 -> jpeg(bytes)
    bytes.size >= 12 && bytes.ascii(0, 4) == "RIFF" && bytes.ascii(8, 4) == "WEBP" -> webp(bytes)
    else -> null
  }

  // The IHDR chunk comes first: its width and height follow the signature, length and type
  private fun png(bytes: ByteArray): ImageSize? {
    if (bytes.size < 24 || bytes.ascii(12, 4) != "IHDR") return null
    return sized(bytes.u32be(16), bytes.u32be(20))
  }

  // Segments follow one another until a start-of-frame, which holds the height then the width
  private fun jpeg(bytes: ByteArray): ImageSize? {
    var index = 2
    while (index + 4 <= bytes.size) {
      if (bytes.u8(index) != 0xFF) return null
      val marker = bytes.u8(index + 1)
      when {
        // Fill bytes before a marker
        marker == 0xFF -> index++
        // Markers that stand alone, with no length
        marker == 0x01 || marker in 0xD0..0xD8 -> index += 2
        marker in StartOfFrameMarkers -> {
          if (index + 9 > bytes.size) return null
          return sized(width = bytes.u16be(index + 7), height = bytes.u16be(index + 5))
        }
        // The image data starts without a frame having been seen
        marker == 0xDA || marker == 0xD9 -> return null
        else -> index += 2 + bytes.u16be(index + 2)
      }
    }
    return null
  }

  private fun webp(bytes: ByteArray): ImageSize? {
    if (bytes.size < 30) return null
    return when (bytes.ascii(12, 4)) {
      // Lossy: 14 bits each, after the frame tag and start code
      "VP8 " -> sized(bytes.u16le(26) and 0x3FFF, bytes.u16le(28) and 0x3FFF)
      // Lossless: 14 bits each, less one, packed after the signature byte
      "VP8L" -> {
        val b1 = bytes.u8(22)
        val b2 = bytes.u8(23)
        val b3 = bytes.u8(24)
        sized(
          width = 1 + (bytes.u8(21) or ((b1 and 0x3F) shl 8)),
          height = 1 + ((b1 shr 6) or (b2 shl 2) or ((b3 and 0x0F) shl 10)),
        )
      }
      // Extended: 24 bits each, less one, after the flags
      "VP8X" -> sized(1 + bytes.u24le(24), 1 + bytes.u24le(27))
      else -> null
    }
  }

  private fun sized(width: Int, height: Int): ImageSize? =
    ImageSize(width, height).takeIf { width > 0 && height > 0 }

  private val PngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

  // Every start-of-frame but the ones reused for Huffman and arithmetic tables, and JPG extensions
  private val StartOfFrameMarkers = (0xC0..0xCF).toSet() - setOf(0xC4, 0xC8, 0xCC)

  private fun ByteArray.startsWith(prefix: ByteArray) =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

  private fun ByteArray.u8(index: Int) = this[index].toInt() and 0xFF

  private fun ByteArray.u16be(index: Int) = (u8(index) shl 8) or u8(index + 1)

  private fun ByteArray.u16le(index: Int) = u8(index) or (u8(index + 1) shl 8)

  private fun ByteArray.u24le(index: Int) = u8(index) or (u8(index + 1) shl 8) or (u8(index + 2) shl 16)

  private fun ByteArray.u32be(index: Int) =
    (u8(index) shl 24) or (u8(index + 1) shl 16) or (u8(index + 2) shl 8) or u8(index + 3)

  private fun ByteArray.ascii(index: Int, length: Int) =
    (index until index + length).joinToString("") { this[it].toInt().toChar().toString() }
}
