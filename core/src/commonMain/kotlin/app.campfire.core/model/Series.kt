package app.campfire.core.model

data class Series(
  val id: String,
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
  val firstBookUnread: LibraryItem? = null,
)
