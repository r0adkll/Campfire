package app.campfire.db.test.model

import app.campfire.data.Series

fun createDbSeries(
  id: String,
  name: String = "Db Series: $id",
  description: String? = "Test Db Series",
  addedAt: Long = 0L,
  updatedAt: Long = 0L,
  inProgress: Boolean = false,
  hasActiveBook: Boolean = false,
  hideFromContinueListening: Boolean = false,
  bookInProgressLastUpdate: Long? = null,
  firstBookUnreadId: String? = null,
  libraryId: String = "test_db_library_id",
  userId: String = "test_user_id",
) = Series(
  id = id,
  name = name,
  description = description,
  addedAt = addedAt,
  updatedAt = updatedAt,
  inProgress = inProgress,
  hasActiveBook = hasActiveBook,
  hideFromContinueListening = hideFromContinueListening,
  bookInProgressLastUpdate = bookInProgressLastUpdate,
  firstBookUnreadId = firstBookUnreadId,
  libraryId = libraryId,
  userId = userId,
)
