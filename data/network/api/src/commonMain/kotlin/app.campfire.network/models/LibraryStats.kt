package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class LargestItem(
  val id: String,
  val title: String,
  val size: Long,
)

@Serializable
data class AuthorWithCount(
  val id: String,
  val name: String,
  val count: Int,
)

@Serializable
data class GenreWithCount(
  val genre: String,
  val count: Int,
)

@Serializable
data class LongestItem(
  val id: String,
  val title: String,
  val duration: Double,
)

@Serializable
data class LibraryStats(
  val largestItems: List<LargestItem>,
  val totalAuthors: Int,
  val authorsWithCount: List<AuthorWithCount>,
  val totalGenres: Int,
  val genresWithCount: List<GenreWithCount>,
  val totalItems: Int,
  val longestItems: List<LongestItem>,
  val totalSize: Long,
  val totalDuration: Double,
  val numAudioTracks: Int,
)
