// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.sortedByName
import app.campfire.core.model.toSeriesSequenceOrNull
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.MinifiedBookMetadata

fun MinifiedBookMetadata.asDomainModel(): Media.Metadata.Book {
  return Media.Metadata.Book(
    title = title,
    titleIgnorePrefix = titleIgnorePrefix,
    subtitle = subtitle,
    authorName = authorName,
    authorNameLastFirst = authorNameLF,
    narratorName = narratorName,
    seriesName = seriesName,
    series = listOfNotNull(
      series?.let {
        SeriesSequence(
          id = it.id,
          name = it.name,
          sequence = it.sequence.toSeriesSequenceOrNull() ?: SeriesSequence.UNKNOWN_SEQUENCE,
        )
      },
    ),
    genres = genres ?: emptyList(),
    publishedYear = publishedYear,
    publishedDate = publishedDate,
    publisher = publisher,
    description = description,
    ISBN = isbn,
    ASIN = asin,
    language = language,
    isExplicit = explicit,
    isAbridged = abridged,
  )
}

fun ExpandedBookMetadata.asDomainModel(): Media.Metadata.Book {
  return Media.Metadata.Book(
    title = title,
    titleIgnorePrefix = titleIgnorePrefix,
    subtitle = subtitle,
    authorName = authorName,
    authorNameLastFirst = authorNameLF,
    narratorName = narratorName ?: narrators?.firstOrNull(),
    seriesName = seriesName,
    authors = authors?.map {
      Media.AuthorMetadata(
        id = it.id,
        name = it.name,
      )
    } ?: emptyList(),
    series = series?.map {
      SeriesSequence(
        id = it.id,
        name = it.name,
        sequence = it.sequence.toSeriesSequenceOrNull() ?: SeriesSequence.UNKNOWN_SEQUENCE,
      )
    }?.sortedByName() ?: emptyList(),
    narrators = narrators ?: emptyList(),
    genres = genres ?: emptyList(),
    publishedYear = publishedYear,
    publishedDate = publishedDate,
    publisher = publisher,
    description = description,
    ISBN = isbn,
    ASIN = asin,
    language = language,
    isExplicit = explicit,
    isAbridged = abridged,
  )
}
