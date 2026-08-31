// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeries
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.store.mergeSeriesEntries
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.SeriesSequence
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

private fun owned(
  title: String,
  isbn: String? = null,
  asin: String? = null,
  sequence: Double? = null,
  id: String = "item_$title",
): LibraryItem = libraryItem(
  id = id,
  media = media(
    metadata = mediaMetadata(
      title = title,
      ISBN = isbn,
      ASIN = asin,
      seriesSequence = sequence?.let { SeriesSequence(id = "s", name = "Series", sequence = it) },
    ),
  ),
)

private fun providerEntry(
  title: String,
  position: Double,
  isReleased: Boolean = true,
  isbns: List<String> = emptyList(),
  asins: List<String> = emptyList(),
) = ProviderSeriesEntry(
  providerBookId = "hc_$title",
  position = position,
  title = title,
  releaseDate = if (isReleased) "2010-08-31" else "2031-01-01",
  isReleased = isReleased,
  providerUrl = null,
  coverUrl = null,
  isbns = isbns,
  asins = asins,
)

private fun series(vararg entries: ProviderSeriesEntry) = ProviderSeries(
  providerSeriesId = "997",
  name = "The Stormlight Archive",
  isCompleted = false,
  entries = entries.toList(),
)

class SeriesMergeTest {

  @Test
  fun `owned books match provider entries by isbn`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("The Way of Kings", isbn = "978-0-7653-9304-3")),
      series = series(providerEntry("The Way of Kings", 1.0, isbns = listOf("9780765393043"))),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.size).isEqualTo(1)
    assertThat(entries.single()).isInstanceOf<SeriesEntry.Owned>()
  }

  @Test
  fun `owned books match provider entries by asin`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("Some Title", asin = "b002ri9z9e")),
      series = series(providerEntry("Different Title", 1.0, asins = listOf("B002RI9Z9E"))),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.single()).isInstanceOf<SeriesEntry.Owned>()
  }

  @Test
  fun `owned books match by normalized title when identifiers differ`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("The Way of Kings")),
      series = series(providerEntry("Way of Kings", 1.0, isbns = listOf("9780765393043"))),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.single()).isInstanceOf<SeriesEntry.Owned>()
  }

  @Test
  fun `unowned released entries are missing and unreleased are upcoming`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("The Way of Kings", isbn = "9780765393043")),
      series = series(
        providerEntry("The Way of Kings", 1.0, isbns = listOf("9780765393043")),
        providerEntry("Words of Radiance", 2.0),
        providerEntry("Untitled #6", 6.0, isReleased = false),
      ),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.map { it::class.simpleName }).isEqualTo(
      listOf("Owned", "Missing", "Upcoming"),
    )
  }

  @Test
  fun `entries are ordered by provider position`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("Words of Radiance", isbn = "9780765326379")),
      series = series(
        providerEntry("Words of Radiance", 2.0, isbns = listOf("9780765326379")),
        providerEntry("The Way of Kings", 1.0),
        providerEntry("Edgedancer", 2.5),
      ),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.map { it.title() })
      .isEqualTo(listOf("The Way of Kings", "Words of Radiance", "Edgedancer"))
  }

  @Test
  fun `an owned book is never also listed as missing`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("The Way of Kings", isbn = "9780765393043")),
      series = series(
        providerEntry("The Way of Kings", 1.0, isbns = listOf("9780765393043")),
        providerEntry("The Way of Kings", 1.1),
      ),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.count { it is SeriesEntry.Owned }).isEqualTo(1)
    // The duplicate provider row can't consume the same owned book twice.
    assertThat(entries.count { it is SeriesEntry.Missing }).isEqualTo(1)
  }

  @Test
  fun `owned books the provider does not list are kept`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(
        owned("The Way of Kings", isbn = "9780765393043", sequence = 1.0),
        owned("Fan Companion", sequence = 3.0),
      ),
      series = series(providerEntry("The Way of Kings", 1.0, isbns = listOf("9780765393043"))),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.map { it.title() }).isEqualTo(listOf("The Way of Kings", "Fan Companion"))
    assertThat(entries.all { it is SeriesEntry.Owned }).isEqualTo(true)
  }

  @Test
  fun `without a provider series only owned books are listed in sequence order`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(
        owned("Words of Radiance", sequence = 2.0),
        owned("The Way of Kings", sequence = 1.0),
      ),
      series = null,
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.map { it.title() })
      .isEqualTo(listOf("The Way of Kings", "Words of Radiance"))
    assertThat(entries.all { it is SeriesEntry.Owned }).isEqualTo(true)
  }

  @Test
  fun `entry keys are unique across the listing`() {
    val entries = mergeSeriesEntries(
      ownedItems = listOf(owned("The Way of Kings", isbn = "9780765393043")),
      series = series(
        providerEntry("The Way of Kings", 1.0, isbns = listOf("9780765393043")),
        providerEntry("Words of Radiance", 2.0),
        providerEntry("Untitled #6", 6.0, isReleased = false),
      ),
      providerId = ProviderId.Hardcover,
    )

    assertThat(entries.map { it.key }.toSet().size).isEqualTo(entries.size)
  }
}

private fun SeriesEntry.title(): String? = when (this) {
  is SeriesEntry.Owned -> item.media.metadata.title
  is SeriesEntry.Missing -> entry.title
  is SeriesEntry.Upcoming -> entry.title
}
