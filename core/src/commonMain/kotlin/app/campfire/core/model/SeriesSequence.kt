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
private val MULTI_PART_SEQUENCE_REGEX = "^\\s*\\d+(?:\\.\\d+){2,}".toRegex()
private val YEAR_MONTH_SEQUENCE_REGEX = "^\\s*\\d{4}\\.(?:0?[1-9]|1[0-2])\\s*$".toRegex()

/**
 * Decimal reading order. Matches the server's `CAST(sequence AS FLOAT)` order (so `1.25` sits
 * between `1` and `1.5`, and `1.10` before `1.9`), then breaks the ties that cast leaves by
 * comparing each run of digits in the displayed sequence as a number, which keeps entries like
 * `2026.10.01` and `2026.10.10` in order.
 */
val SeriesSequenceOrder: Comparator<SeriesSequence> =
  compareBy<SeriesSequence> { it.sequence }
    .thenComparator { a, b -> compareSequenceTokens(a.formattedSequence, b.formattedSequence) }

/**
 * Part-by-part reading order for series numbered like dates or versions, where `2026.9.30`
 * comes before `2026.10.01` because each run of digits is compared as a number. Sequences
 * without a leading number sort last.
 */
val SeriesSequencePartsOrder: Comparator<SeriesSequence> =
  compareBy<SeriesSequence> { it.sequence == SeriesSequence.UNKNOWN_SEQUENCE }
    .thenComparator { a, b -> compareSequenceTokens(a.formattedSequence, b.formattedSequence) }

/**
 * The reading order for a whole series. It sorts part by part ([SeriesSequencePartsOrder]) when
 * the series is numbered by dates or versions, and otherwise keeps the server's decimal order
 * ([SeriesSequenceOrder]). A series counts as dated when:
 * - any sequence has two or more dots (`2026.9.30`), which a decimal can't, or
 * - every sequence with a dot reads as a year and month (`2026.9`, `2026.10`). Decimal series
 *   don't number books in the thousands with month-sized fractions.
 *
 * Choosing once per series, rather than per pair, keeps the order consistent across every
 * comparison.
 */
fun seriesSequenceOrder(sequences: Iterable<SeriesSequence>): Comparator<SeriesSequence> {
  val labels = sequences.map { it.formattedSequence }
  val multiPart = labels.any { MULTI_PART_SEQUENCE_REGEX.containsMatchIn(it) }
  val dotted = labels.filter { '.' in it }
  val yearMonth = dotted.isNotEmpty() && dotted.all { YEAR_MONTH_SEQUENCE_REGEX.matches(it) }
  return if (multiPart || yearMonth) SeriesSequencePartsOrder else SeriesSequenceOrder
}

/**
 * Sort a series' books by their position in [seriesId] (falling back to their primary series
 * when that entry is missing), using [seriesSequenceOrder] for the series. Books with no
 * series come first, and books at the same position fall back to their title.
 */
fun Iterable<LibraryItem>.sortedBySeriesSequence(seriesId: SeriesId): List<LibraryItem> {
  val positions = associateWith { item ->
    val metadata = item.media.metadata
    metadata.seriesSequence(seriesId) ?: metadata.seriesSequence
  }
  val order = seriesSequenceOrder(positions.values.filterNotNull())
  return sortedWith(
    compareBy<LibraryItem, SeriesSequence?>(nullsFirst(order)) { positions[it] }
      .thenBy(String.CASE_INSENSITIVE_ORDER) { it.media.metadata.title.orEmpty() },
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
