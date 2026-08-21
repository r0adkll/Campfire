// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

data class SeriesSequence(
  val id: String,
  val name: String,
  /**
   * Position of the book within the series. Audiobookshelf stores this as free-form
   * text, so fractional entries like `1.5` are common; anything that can't be parsed
   * sorts after every numbered entry via [UNKNOWN_SEQUENCE].
   */
  val sequence: Double,
) {
  /**
   * The sequence formatted the way the server would present it: whole numbers drop the
   * trailing `.0`, fractional numbers keep their decimals.
   */
  val formattedSequence: String
    get() = formatSeriesSequence(sequence)

  companion object {
    /** Sort key used for books whose sequence is missing or unparseable. */
    const val UNKNOWN_SEQUENCE: Double = Double.MAX_VALUE
  }
}

/**
 * Canonical ordering for the series a book belongs to. The Audiobookshelf server builds
 * a book's series array from an unordered join, so its order can differ between requests;
 * sort by name (then id) so the list is deterministic everywhere it's shown or stored.
 */
fun List<SeriesSequence>.sortedByName(): List<SeriesSequence> {
  return sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
}

private val LEADING_NUMBER_REGEX = "^\\s*(\\d+(?:\\.\\d+)?)".toRegex()

/**
 * Parse a server-provided series sequence string into a sortable number. Accepts plain
 * integers (`"3"`), decimals (`"1.5"`), and values with a numeric prefix (`"2a"`, `"4-5"`)
 * by using the leading number. Returns null for blank or non-numeric input.
 */
fun String?.toSeriesSequenceOrNull(): Double? {
  if (this.isNullOrBlank()) return null
  return trim().toDoubleOrNull()
    ?: LEADING_NUMBER_REGEX.find(this)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
}

fun formatSeriesSequence(sequence: Double): String {
  if (sequence == SeriesSequence.UNKNOWN_SEQUENCE) return ""
  val whole = sequence.toLong()
  return if (whole.toDouble() == sequence) whole.toString() else sequence.toString()
}
