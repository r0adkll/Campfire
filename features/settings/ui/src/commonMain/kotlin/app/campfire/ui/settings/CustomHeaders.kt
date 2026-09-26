// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings

/**
 * Returns these headers with [name] set to [value], replacing [originalName] when editing an
 * existing header. Header names are case-insensitive, so any other spelling of [name] is replaced
 * rather than duplicated.
 */
internal fun Map<String, String>.withHeader(
  originalName: String?,
  name: String,
  value: String,
): Map<String, String> {
  val cleanName = name.trim()
  return filterKeys { existing ->
    !existing.equals(originalName, ignoreCase = true) && !existing.equals(cleanName, ignoreCase = true)
  } + (cleanName to value.trim())
}

/** Why a header can't be saved, or null when it can. */
internal enum class HeaderError {
  MissingName,
  InvalidName,
  MissingValue,
  InvalidValue,
}

/**
 * Validates a header before it's stored. Names must be HTTP tokens (RFC 9110); values must be
 * non-empty, single-line, and free of the sequences the header store uses as separators.
 */
internal fun validateHeader(name: String, value: String): HeaderError? {
  val cleanName = name.trim()
  val cleanValue = value.trim()
  return when {
    cleanName.isEmpty() -> HeaderError.MissingName
    !cleanName.all { it in TOKEN_CHARS } -> HeaderError.InvalidName
    cleanValue.isEmpty() -> HeaderError.MissingValue
    cleanValue.any { it == '\n' || it == '\r' } ||
      STORE_SEPARATORS.any { it in cleanValue } -> HeaderError.InvalidValue
    else -> null
  }
}

private val TOKEN_CHARS: Set<Char> =
  (('a'..'z') + ('A'..'Z') + ('0'..'9') + "!#$%&'*+-.^_`|~".toList()).toSet()

// Mirrors SecureExtraHeaderStorage's encoding, which can't represent these inside a value
private val STORE_SEPARATORS = listOf(";|;", ":|:")
