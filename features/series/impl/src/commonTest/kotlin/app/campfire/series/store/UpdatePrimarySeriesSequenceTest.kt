// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.store

import app.campfire.CampfireDatabase
import app.campfire.core.model.SeriesSequence
import app.campfire.data.Media
import app.campfire.db.test.testDb
import app.campfire.db.updatePrimarySeriesSequence
import app.cash.sqldelight.async.coroutines.awaitAsOne
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import kotlin.test.Test

class UpdatePrimarySeriesSequenceTest {

  private val sessions = SeriesSequence("sessions", "D&D Sessions", 2026.1, label = "2026.10.10")

  @Test
  fun `cached row gains the labelled entry and keeps its other series`() = testDb { db ->
    val other = SeriesSequence("other", "Other", 3.0, label = "3")
    // Cached from a listing that knew nothing about the D&D series.
    db.mediaQueries.insert(dbMedia(series = listOf(other)))

    db.mediaQueries.updatePrimarySeriesSequence(dbMedia(primary = sessions, series = listOf(sessions)))

    assertThat(db.storedSeries()).containsExactlyInAnyOrder(other, sessions)
  }

  @Test
  fun `existing entry for the series is replaced`() = testDb { db ->
    db.mediaQueries.insert(dbMedia(series = listOf(sessions.copy(label = null))))

    db.mediaQueries.updatePrimarySeriesSequence(dbMedia(primary = sessions, series = listOf(sessions)))

    assertThat(db.storedSeries()).containsExactlyInAnyOrder(sessions)
  }

  @Test
  fun `row without a primary series is left alone`() = testDb { db ->
    val other = SeriesSequence("other", "Other", 3.0, label = "3")
    db.mediaQueries.insert(dbMedia(series = listOf(other)))

    db.mediaQueries.updatePrimarySeriesSequence(dbMedia(series = listOf(sessions)))

    assertThat(db.storedSeries()).containsExactlyInAnyOrder(other)
  }

  private suspend fun CampfireDatabase.storedSeries(): List<SeriesSequence> =
    mediaQueries.selectSeriesList(LIBRARY_ITEM_ID).awaitAsOne().metadata_series.orEmpty()

  private fun dbMedia(
    primary: SeriesSequence? = null,
    series: List<SeriesSequence>? = null,
  ) = Media(
    mediaId = "media",
    coverPath = null,
    tags = null,
    numTracks = 0,
    numAudioFiles = 0,
    numChapters = 0,
    numMissingParts = 0,
    numInvalidAudioFiles = 0,
    durationInMillis = 0L,
    sizeInBytes = 0L,
    propertySize = null,
    ebookFormat = null,
    metadata_title = "Book",
    metadata_subtitle = null,
    metadata_genres = null,
    metadata_publishedYear = null,
    metadata_publishedDate = null,
    metadata_publisher = null,
    metadata_description = null,
    metadata_isbn = null,
    metadata_asin = null,
    metadata_language = null,
    metadata_explicit = false,
    metadata_abridged = false,
    metadata_titleIgnorePrefix = null,
    metadata_authorName = null,
    metadata_authorNameLF = null,
    metadata_narratorName = null,
    metadata_seriesName = null,
    metadata_series_id = primary?.id,
    metadata_series_name = primary?.name,
    metadata_series_sequence = primary?.sequence,
    libraryItemId = LIBRARY_ITEM_ID,
    metadata_series = series,
  )

  private companion object {
    const val LIBRARY_ITEM_ID = "item"
  }
}
