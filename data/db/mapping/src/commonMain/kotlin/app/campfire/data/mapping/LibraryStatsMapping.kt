package app.campfire.data.mapping

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.AuthorWithCount
import app.campfire.core.model.GenreWithCount
import app.campfire.core.model.LargestItem
import app.campfire.core.model.LibraryStats
import app.campfire.core.model.LongestItem
import app.campfire.network.models.LibraryStats as NetworkLibraryStats
import kotlin.time.Duration.Companion.seconds

suspend fun NetworkLibraryStats.asDomainModel(
  tokenHydrator: TokenHydrator,
): LibraryStats {
  return LibraryStats(
    largestItems = largestItems.map { item ->
      LargestItem(
        id = item.id,
        title = item.title,
        coverImageUrl = tokenHydrator.hydrateLibraryItem(item.id),
        sizeInBytes = item.size,
      )
    },
    totalAuthors = totalAuthors,
    authorsWithCount = authorsWithCount.map { author ->
      AuthorWithCount(
        id = author.id,
        name = author.name,
        imageUrl = tokenHydrator.hydrateAuthor(author.id),
        count = author.count,
      )
    },
    totalGenres = totalGenres,
    genresWithCount = genresWithCount.map { genre ->
      GenreWithCount(genre.genre, genre.count)
    },
    totalItems = totalItems,
    longestItems = longestItems.map { item ->
      LongestItem(
        id = item.id,
        title = item.title,
        coverImageUrl = tokenHydrator.hydrateLibraryItem(item.id),
        duration = item.duration.seconds,
      )
    },
    totalSizeInBytes = totalSize,
    totalDuration = totalDuration.seconds,
    numAudioTracks = numAudioTracks,
  )
}
