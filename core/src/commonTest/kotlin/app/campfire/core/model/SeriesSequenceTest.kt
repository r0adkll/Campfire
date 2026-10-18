// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.model.preview.libraryItem
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class SeriesSequenceTest {

  @Test
  fun parsesWholeAndDecimalSequences() {
    assertThat("3".toSeriesSequenceOrNull()).isEqualTo(3.0)
    assertThat("1.1".toSeriesSequenceOrNull()).isEqualTo(1.1)
    assertThat(" 2.5 ".toSeriesSequenceOrNull()).isEqualTo(2.5)
  }

  @Test
  fun parsesLeadingNumberFromSuffixedSequences() {
    assertThat("2a".toSeriesSequenceOrNull()).isEqualTo(2.0)
    assertThat("4-5".toSeriesSequenceOrNull()).isEqualTo(4.0)
    assertThat("1.5b".toSeriesSequenceOrNull()).isEqualTo(1.5)
  }

  @Test
  fun returnsNullForBlankOrNonNumeric() {
    assertThat(null.toSeriesSequenceOrNull()).isNull()
    assertThat("".toSeriesSequenceOrNull()).isNull()
    assertThat("   ".toSeriesSequenceOrNull()).isNull()
    assertThat("prequel".toSeriesSequenceOrNull()).isNull()
  }

  @Test
  fun decimalSequencesSortBetweenWholeNumbers() {
    val sorted = listOf("2", "1.2", "1", "1.1", "10", "prequel")
      .map { SeriesSequence(it, it, it.toSeriesSequenceOrNull() ?: SeriesSequence.UNKNOWN_SEQUENCE) }
      .sortedBy { it.sequence }
      .map { it.id }

    assertThat(sorted).containsExactly("1", "1.1", "1.2", "2", "10", "prequel")
  }

  @Test
  fun formatsSequenceLikeTheServer() {
    assertThat(formatSeriesSequence(3.0)).isEqualTo("3")
    assertThat(formatSeriesSequence(1.5)).isEqualTo("1.5")
    assertThat(formatSeriesSequence(SeriesSequence.UNKNOWN_SEQUENCE)).isEqualTo("")
    assertThat(SeriesSequence("id", "name", 2.0).formattedSequence).isEqualTo("2")
  }

  @Test
  fun formattedSequence_prefersServerLabel() {
    val dated = SeriesSequence("id", "name", "2026.10.10".toSeriesSequenceOrNull()!!, label = "2026.10.10")

    assertThat(dated.formattedSequence).isEqualTo("2026.10.10")
    assertThat(SeriesSequence("id", "name", 2.0, label = " 02 ").formattedSequence).isEqualTo("02")
    assertThat(SeriesSequence("id", "name", 2.0, label = " ").formattedSequence).isEqualTo("2")
  }

  @Test
  fun seriesSequenceOrder_matchesServerCastOrder() {
    val sorted = listOf("2", "1.9", "1.10", "1.5", "1", "prequel")
      .map(::sequence)
      .sortedWith(SeriesSequenceOrder)
      .map { it.formattedSequence }

    // CAST(sequence AS FLOAT) reads 1.10 as 1.1, so it comes before 1.9.
    assertThat(sorted).containsExactly("1", "1.10", "1.5", "1.9", "2", "prequel")
  }

  @Test
  fun seriesSequenceOrder_breaksCastTiesByEachNumber() {
    val sorted = listOf("2026.10.10", "2026.10.02", "2026.1", "2026.10", "2026.10.01b", "2026.10.01")
      .map(::sequence)
      .sortedWith(SeriesSequenceOrder)
      .map { it.formattedSequence }

    assertThat(sorted).containsExactly("2026.1", "2026.10", "2026.10.01", "2026.10.01b", "2026.10.02", "2026.10.10")
  }

  @Test
  fun seriesSequenceOrder_comparesLongDigitRunsByValue() {
    // Both parse to the same Double, so only the digit-run comparison can order them.
    val sorted = listOf("1.100000000000000000002", "1.100000000000000000001")
      .map(::sequence)
      .sortedWith(SeriesSequenceOrder)
      .map { it.formattedSequence }

    assertThat(sorted).containsExactly("1.100000000000000000001", "1.100000000000000000002")
  }

  @Test
  fun sortedBySeriesSequence_ordersByPositionInRequestedSeries() {
    fun book(id: String, sessions: String, other: String) = libraryItem(id = id).withSeries(
      sequence(sessions, seriesId = "sessions"),
      sequence(other, seriesId = "other"),
    )
    val items = listOf(
      book("c", sessions = "2026.10.10", other = "1"),
      book("a", sessions = "2026.10.01", other = "3"),
      book("b", sessions = "2026.10.02", other = "2"),
    )

    assertThat(items.sortedBySeriesSequence("sessions").map { it.id }).containsExactly("a", "b", "c")
    assertThat(items.sortedBySeriesSequence("other").map { it.id }).containsExactly("c", "b", "a")
  }

  @Test
  fun sortedBySeriesSequence_usesPrimarySeriesAndTitleForTies() {
    val items = listOf(
      libraryItem(id = "zed").withSeries(sequence("2")).withTitle("Zed"),
      libraryItem(id = "none"),
      libraryItem(id = "alpha").withSeries(sequence("2")).withTitle("alpha"),
      libraryItem(id = "first").withSeries(sequence("1")),
    )

    assertThat(items.sortedBySeriesSequence().map { it.id }).containsExactly("none", "first", "alpha", "zed")
  }

  private fun sequence(label: String, seriesId: String = "id") = SeriesSequence(
    id = seriesId,
    name = seriesId,
    sequence = label.toSeriesSequenceOrNull() ?: SeriesSequence.UNKNOWN_SEQUENCE,
    label = label,
  )

  private fun LibraryItem.withMetadata(
    transform: (Media.Metadata.Book) -> Media.Metadata.Book,
  ): LibraryItem {
    val book = media as Media.Book
    return copy(media = book.copy(metadata = transform(book.metadata as Media.Metadata.Book)))
  }

  private fun LibraryItem.withSeries(vararg series: SeriesSequence) = withMetadata { it.copy(series = series.toList()) }

  private fun LibraryItem.withTitle(title: String) = withMetadata { it.copy(title = title) }

  @Test
  fun seriesSequenceForSeriesId_picksThatSeriesEntry() {
    val cosmere = SeriesSequence("cosmere", "Cosmere", 12.0)
    val stormlight = SeriesSequence("stormlight", "The Stormlight Archive", 2.0)
    val metadata = (libraryItem().media.metadata as Media.Metadata.Book)
      .copy(series = listOf(cosmere, stormlight))

    assertThat(metadata.seriesSequence("stormlight")).isEqualTo(stormlight)
    assertThat(metadata.seriesSequence("mistborn")).isNull()
  }
}
