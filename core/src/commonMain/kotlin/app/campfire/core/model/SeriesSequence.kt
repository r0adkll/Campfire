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
  /**
   * The sequence exactly as the server stores it (e.g. `2026.10.10`), for display. Null when
   * only the parsed [sequence] is known.
   */
  val label: String? = null,
) {
  /**
   * The sequence as it should be displayed: the server's own [label] when there is one,
   * otherwise [sequence] with whole numbers dropping the trailing `.0`.
   */
  val formattedSequence: String
    get() = label?.trim()?.takeIf { it.isNotEmpty() } ?: formatSeriesSequence(sequence)

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

private val SEQUENCE_TOKEN_REGEX = "\\d+|\\D+".toRegex()

/**
 * Reading order for books in a series. Matches the server's `CAST(sequence AS FLOAT)` order
 * (so `1.5` sits between `1` and `2`, and `1.10` before `1.9`), then breaks the ties that cast
 * leaves by comparing each run of digits in the displayed sequence as a number, which keeps
 * dated entries like `2026.10.01` and `2026.10.10` in order.
 */
val SeriesSequenceOrder: Comparator<SeriesSequence> =
  compareBy<SeriesSequence> { it.sequence }
    .thenComparator { a, b -> compareSequenceTokens(a.formattedSequence, b.formattedSequence) }

/**
 * Sort books by their position in [seriesId], or in their primary series when null. Books
 * outside the series come first, and books at the same position fall back to their title.
 */
fun Iterable<LibraryItem>.sortedBySeriesSequence(seriesId: SeriesId? = null): List<LibraryItem> {
  return sortedWith(
    compareBy<LibraryItem, SeriesSequence?>(nullsFirst(SeriesSequenceOrder)) { item ->
      val metadata = item.media.metadata
      if (seriesId != null) metadata.seriesSequence(seriesId) else metadata.seriesSequence
    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.media.metadata.title.orEmpty() },
  )
}

private fun compareSequenceTokens(a: String, b: String): Int {
  val aTokens = SEQUENCE_TOKEN_REGEX.findAll(a).map { it.value }.iterator()
  val bTokens = SEQUENCE_TOKEN_REGEX.findAll(b).map { it.value }.iterator()
  while (aTokens.hasNext() && bTokens.hasNext()) {
    val aToken = aTokens.next()
    val bToken = bTokens.next()
    val result = if (aToken[0].isDigit() && bToken[0].isDigit()) {
      // Compare digit runs by value without parsing, so runs of any length can't overflow.
      val aDigits = aToken.trimStart('0')
      val bDigits = bToken.trimStart('0')
      compareValuesBy(aDigits, bDigits, { it.length }, { it })
    } else {
      aToken.compareTo(bToken, ignoreCase = true)
    }
    if (result != 0) return result
  }
  return aTokens.hasNext().compareTo(bTokens.hasNext())
}
