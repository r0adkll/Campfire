// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

fun String.capitalized(): String {
  return lowercase()
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

inline fun String?.ifNullOrBlank(defaultValue: () -> String): String {
  return if (isNullOrBlank()) defaultValue() else this
}

inline fun String?.toIntOrElse(defaultValue: () -> Int): Int {
  return this?.toIntOrNull() ?: defaultValue()
}
