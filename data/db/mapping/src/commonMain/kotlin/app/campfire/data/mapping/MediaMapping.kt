package app.campfire.data.mapping

import app.campfire.core.extensions.toIntOrElse
import app.campfire.core.model.Media
import app.campfire.core.model.SeriesSequence
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.MinifiedBookMetadata

fun MinifiedBookMetadata.asDomainModel(): Media.Metadata {
  return Media.Metadata(
    title = title,
    titleIgnorePrefix = titleIgnorePrefix,
    subtitle = subtitle,
    authorName = authorName,
    authorNameLastFirst = authorNameLF,
    narratorName = narratorName,
    seriesName = seriesName,
    seriesSequence = series?.let {
      SeriesSequence(
        id = it.id,
        name = it.name,
        sequence = it.sequence.toIntOrElse { Int.MAX_VALUE },
      )
    },
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

fun ExpandedBookMetadata.asDomainModel(): Media.Metadata {
  return Media.Metadata(
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
    seriesSequence = series?.map {
      SeriesSequence(
        id = it.id,
        name = it.name,
        sequence = it.sequence.toIntOrElse { Int.MAX_VALUE },
      )
    }?.firstOrNull(),
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
