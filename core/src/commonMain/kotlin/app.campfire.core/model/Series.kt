package app.campfire.core.model

typealias SeriesId = String

data class Series(
  val id: SeriesId,
  val name: String,
  val description: String?,
  val addedAt: Long,
  val updatedAt: Long,

  // SeriesBook
  val books: List<LibraryItem>? = null,
  val nameIgnorePrefix: String? = null,
  val nameIgnorePrefixSort: String? = null,
  val totalDurationInMillis: Long? = null,

  // SeriesPersonalized
  val inProgress: Boolean = false,
  val hasActiveBook: Boolean = false,
  val hideFromContinueListening: Boolean = false,
  val bookInProgressLastUpdate: Long? = null,
  val firstBookUnreadId: LibraryItemId? = null,
)
