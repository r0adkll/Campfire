// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> Enum<T>.next(): T {
  val nextOrdinal = (ordinal + 1) % enumEntries<T>().size
  return enumEntries<T>()[nextOrdinal]
}

inline fun <reified T : Enum<T>> Enum<T>.previous(): T {
  val size = enumEntries<T>().size
  val nextOrdinal = ((ordinal + size) - 1) % size
  return enumEntries<T>()[nextOrdinal]
}
