package app.campfire.core.model

import kotlin.time.Duration

data class LargestItem(
  val id: String,
  val title: String,
  val coverImageUrl: String,
  val sizeInBytes: Long,
)

data class AuthorWithCount(
  val id: String,
  val name: String,
  val imageUrl: String,
  val count: Int,
)

data class GenreWithCount(
  val genre: String,
  val count: Int,
)

data class LongestItem(
  val id: String,
  val title: String,
  val coverImageUrl: String,
  val duration: Duration,
)

data class LibraryStats(
  val totalItems: Int,
  val totalAuthors: Int,
  val totalGenres: Int,
  val totalSizeInBytes: Long,
  val totalDuration: Duration,
  val numAudioTracks: Int,
  val largestItems: List<LargestItem>,
  val authorsWithCount: List<AuthorWithCount>,
  val genresWithCount: List<GenreWithCount>,
  val longestItems: List<LongestItem>,
)
