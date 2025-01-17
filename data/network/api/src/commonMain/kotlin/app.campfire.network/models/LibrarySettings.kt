package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The settings for the library.
 *
 * @param coverAspectRatio Whether the library should use square book covers. Must be 0 (for false) or 1 (for true).
 * @param disableWatcher Whether to disable the folder watcher for the library.
 * @param skipMatchingMediaWithAsin Whether to skip matching books that already have an ASIN.
 * @param skipMatchingMediaWithIsbn Whether to skip matching books that already have an ISBN.
 * @param autoScanCronExpression The cron expression for when to automatically scan the library folders. If null, automatic scanning will be disabled.
 * @param audiobooksOnly Whether the library should ignore ebook files and only allow ebook files to be supplementary.
 * @param hideSingleBookSeries Whether to hide series with only one book.
 * @param onlyShowLaterBooksInContinueSeries Whether to only show books in a series after the highest series sequence.
 * @param metadataPrecedence The precedence of metadata sources. See Metadata Providers for a list of possible providers.
 * @param podcastSearchRegion The region to use when searching for podcasts.
 */
@Serializable
data class LibrarySettings(
  val coverAspectRatio: Int,
  val disableWatcher: Boolean,
  val skipMatchingMediaWithAsin: Boolean = false,
  val skipMatchingMediaWithIsbn: Boolean = false,
  val autoScanCronExpression: String? = null,
  val audiobooksOnly: Boolean? = null,
  val hideSingleBookSeries: Boolean? = null,
  val onlyShowLaterBooksInContinueSeries: Boolean? = null,
  val metadataPrecedence: List<String>? = null,
  val podcastSearchRegion: String? = null,
)
