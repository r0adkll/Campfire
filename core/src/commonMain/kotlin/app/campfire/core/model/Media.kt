package app.campfire.core.model

import app.campfire.core.extensions.asSeconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

typealias MediaId = String

sealed interface Media {
  val id: MediaId
  val metadata: Metadata
  val coverImageUrl: String
  val coverPath: String?
  val tags: List<String>
  val durationInMillis: Long
  val sizeInBytes: Long

  // Book-only fields exposed with null/empty defaults so existing book-centric
  // call sites (e.g. media.numTracks, media.chapters) keep compiling against the
  // sealed parent. Podcast variants leave them at default.
  val numTracks: Int get() = 0
  val numAudioFiles: Int get() = 0
  val numChapters: Int get() = 0
  val numMissingParts: Int get() = 0
  val numInvalidAudioFiles: Int get() = 0
  val ebookFormat: String? get() = null
  val audioFiles: List<AudioFile> get() = emptyList()
  val chapters: List<Chapter> get() = emptyList()
  val tracks: List<AudioTrack> get() = emptyList()

  val durationInSeconds: Float
    get() = durationInMillis.milliseconds.asSeconds()

  val duration: Duration
    get() = durationInMillis.milliseconds

  data class Book(
    override val id: MediaId,
    override val metadata: Metadata.Book,
    override val coverImageUrl: String,
    override val coverPath: String?,
    override val tags: List<String>,
    override val numTracks: Int,
    override val numAudioFiles: Int,
    override val numChapters: Int,
    override val numMissingParts: Int,
    override val numInvalidAudioFiles: Int,
    override val durationInMillis: Long,
    override val sizeInBytes: Long,
    override val ebookFormat: String? = null,
    override val audioFiles: List<AudioFile> = emptyList(),
    override val chapters: List<Chapter> = emptyList(),
    override val tracks: List<AudioTrack> = emptyList(),
  ) : Media

  data class Podcast(
    override val id: MediaId,
    override val metadata: Metadata.Podcast,
    override val coverImageUrl: String,
    override val coverPath: String?,
    override val tags: List<String>,
    override val sizeInBytes: Long,
    val episodes: List<PodcastEpisode> = emptyList(),
    val numEpisodes: Int = episodes.size,
    val autoDownloadEpisodes: Boolean = false,
    val autoDownloadSchedule: String? = null,
    val lastEpisodeCheckMillis: Long? = null,
    val maxEpisodesToKeep: Int = 0,
    val maxNewEpisodesToDownload: Int = 3,
    val latestEpisodePublishedAtMillis: Long? = null,
  ) : Media {
    override val durationInMillis: Long
      get() = episodes.sumOf { it.durationInMillis }
  }

  /**
   * Common metadata across media types. Truly shared fields are abstract; book- or
   * podcast-only fields are exposed with safe defaults from the parent so call sites
   * accessing them on a [Metadata] reference smart-degrade to null/empty for the
   * variant that doesn't carry them.
   */
  sealed interface Metadata {
    val title: String?
    val titleIgnorePrefix: String?
    val description: String?
    val genres: List<String>
    val language: String?
    val isExplicit: Boolean

    // Book-only fields delegated from the parent — Podcast variant leaves at default.
    val subtitle: String? get() = null
    val authorName: String? get() = null
    val authorNameLastFirst: String? get() = null
    val narratorName: String? get() = null
    val seriesName: String? get() = null
    val seriesSequence: SeriesSequence? get() = null
    val publishedYear: String? get() = null
    val publishedDate: String? get() = null
    val publisher: String? get() = null

    @Suppress("PropertyName")
    val ISBN: String? get() = null

    @Suppress("PropertyName")
    val ASIN: String? get() = null
    val isAbridged: Boolean get() = false
    val authors: List<AuthorMetadata> get() = emptyList()
    val narrators: List<String> get() = emptyList()

    // Podcast-only fields delegated similarly so consumers can read them off the
    // sealed parent when relevant. Book variant leaves at default.
    val author: String? get() = null
    val releaseDate: String? get() = null
    val feedUrl: String? get() = null
    val imageUrl: String? get() = null
    val itunesPageUrl: String? get() = null
    val itunesId: String? get() = null
    val itunesArtistId: String? get() = null
    val podcastType: String? get() = null

    data class Book(
      override val title: String?,
      override val titleIgnorePrefix: String?,
      override val subtitle: String?,
      override val authorName: String?,
      override val authorNameLastFirst: String?,
      override val narratorName: String?,
      override val seriesName: String?,
      override val seriesSequence: SeriesSequence?,
      override val genres: List<String>,
      override val publishedYear: String?,
      override val publishedDate: String?,
      override val publisher: String?,
      override val description: String?,
      @Suppress("PropertyName")
      override val ISBN: String?,
      @Suppress("PropertyName")
      override val ASIN: String?,
      override val language: String?,
      override val isExplicit: Boolean,
      override val isAbridged: Boolean,
      override val authors: List<AuthorMetadata> = emptyList(),
      override val narrators: List<String> = emptyList(),
    ) : Metadata

    data class Podcast(
      override val title: String?,
      override val titleIgnorePrefix: String? = null,
      override val author: String?,
      override val description: String?,
      override val releaseDate: String?,
      override val genres: List<String>,
      override val feedUrl: String?,
      override val imageUrl: String?,
      override val itunesPageUrl: String?,
      override val itunesId: String?,
      override val itunesArtistId: String?,
      override val isExplicit: Boolean,
      override val language: String?,
      override val podcastType: String?,
    ) : Metadata
  }

  data class AuthorMetadata(
    val id: AuthorId,
    val name: String,
  )
}
