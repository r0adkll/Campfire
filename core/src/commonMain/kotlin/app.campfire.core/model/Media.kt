package app.campfire.core.model

typealias MediaId = String

data class Media(
  val id: MediaId,
  val metadata: Metadata,
  val coverImageUrl: String,
  val coverPath: String?,
  val tags: List<String>,
  val numTracks: Int,
  val numAudioFiles: Int,
  val numChapters: Int,
  val numMissingParts: Int,
  val numInvalidAudioFiles: Int,
  val durationInMillis: Long,
  val sizeInBytes: Long,
  val ebookFormat: String? = null,
  val audioFiles: List<AudioFile> = emptyList(),
  val chapters: List<Chapter> = emptyList(),
  val tracks: List<AudioTrack> = emptyList(),
) {
  data class Metadata(
    val title: String?,
    val titleIgnorePrefix: String?,
    val subtitle: String?,
    val authorName: String?,
    val authorNameLastFirst: String?,
    val narratorName: String?,
    val seriesName: String?,
    val seriesSequence: SeriesSequence?,
    val genres: List<String>,
    val publishedYear: String?,
    val publishedDate: String?,
    val publisher: String?,
    val description: String?,
    val ISBN: String?,
    val ASIN: String?,
    val language: String?,
    val isExplicit: Boolean,
    val isAbridged: Boolean,

    val authors: List<AuthorMetadata> = emptyList(),
  )

  data class AuthorMetadata(
    val id: AuthorId,
    val name: String,
  )
}
