package app.campfire.core.model

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
