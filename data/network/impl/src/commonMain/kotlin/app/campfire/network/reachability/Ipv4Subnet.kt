// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

/** Masks [bytes] to [prefixLength] bits and renders it as `a.b.c.d/prefix`. */
internal fun ipv4Subnet(bytes: ByteArray, prefixLength: Int): String? {
  if (bytes.size != 4 || prefixLength !in 0..32) return null
  val address = bytes.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
  val mask = if (prefixLength == 0) 0L else (0xFFFFFFFFL shl (32 - prefixLength)) and 0xFFFFFFFFL
  val network = address and mask
  val octets = (3 downTo 0).joinToString(".") { shift -> ((network shr (shift * 8)) and 0xFF).toString() }
  return "$octets/$prefixLength"
}
