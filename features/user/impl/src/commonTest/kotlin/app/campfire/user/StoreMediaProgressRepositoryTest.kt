// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.session.UserSession
import app.campfire.core.time.FatherTime
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.test.testDb
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class StoreMediaProgressRepositoryTest {

  private val session = UserSession.LoggedIn(user(id = USER_ID))
  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )
  private val api = FakeAudioBookShelfApi().apply {
    updateMediaProgressResult = { Result.success(Unit) }
  }

  private fun repository(db: CampfireDatabase) = StoreMediaProgressRepository(
    userSession = session,
    storeFactory = MediaProgressStore.Factory(api, db, dispatchers),
    db = db,
    api = api,
    libraryItemDao = SqlDelightLibraryItemDao(db, FakeUrlHydrator(), dispatchers, session),
    mediaProgressSynchronizer = object : MediaProgressSynchronizer {
      override suspend fun sync(mediaProgress: MediaProgress, force: Boolean) = Unit
    },
    fatherTime = FixedFatherTime,
    dispatcherProvider = dispatchers,
  )

  @Test
  fun markFinished_FinishesARowStillHoldingThePlaceholderId() = testDb { db ->
    // Playback writes the row before the server has issued the progress its id
    db.mediaProgressQueries.insert(playbackRow(id = MediaProgress.UNKNOWN_ID))

    repository(db).markFinished("item-1")

    val row = db.mediaProgressQueries.selectForEpisode("user-1", "item-1", "").awaitAsOne()
    assertThat(row.isFinished).isTrue()
    assertThat(row.progress).isEqualTo(1.0)
    assertThat(api.mediaProgressUpdates.map { it.isFinished }).containsExactly(true)
  }

  @Test
  fun markFinished_FinishesARowWithItsServerId() = testDb { db ->
    db.mediaProgressQueries.insert(playbackRow(id = "server-progress-id"))

    repository(db).markFinished("item-1")

    val row = db.mediaProgressQueries.selectForEpisode("user-1", "item-1", "").awaitAsOne()
    assertThat(row.isFinished).isTrue()
    assertThat(row.id).isEqualTo("server-progress-id")
  }

  @Test
  fun markAllFinished_sendsOneBatchAndFinishesTheTrackedProgress() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = MediaProgress.UNKNOWN_ID, currentTime = 50f))
    db.insertProgress(progress(PODCAST, episodeId = EPISODE, id = "progress-ep", currentTime = 10f))
    api.batchUpdateMediaProgressResult = { Result.success(Unit) }
    api.mediaProgressResult = { libraryItemId, _ ->
      Result.success(networkProgress(libraryItemId, id = "progress-b", isFinished = true))
    }

    repository(db).markAllFinished(
      listOf(
        MediaProgressKey(BOOK_A),
        MediaProgressKey(BOOK_B),
        MediaProgressKey(PODCAST, EPISODE),
      ),
    )

    assertThat(api.batchMediaProgressUpdates).containsExactly(
      listOf(
        MediaProgressUpdatePayload(libraryItemId = BOOK_A, isFinished = true, finishedAt = NOW),
        MediaProgressUpdatePayload(libraryItemId = BOOK_B, isFinished = true, finishedAt = NOW),
        MediaProgressUpdatePayload(
          libraryItemId = PODCAST,
          episodeId = EPISODE,
          isFinished = true,
          finishedAt = NOW,
        ),
      ),
    )
    // A row still holding the placeholder id is finished like any other
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::isFinished).isTrue()
    assertThat(db.progressFor(PODCAST, EPISODE)).isNotNull().prop(MediaProgress::isFinished).isTrue()
    // The book with no progress picks up the row the server created for it
    assertThat(db.progressFor(BOOK_B)).isNotNull().given { progress ->
      assertThat(progress.id).isEqualTo("progress-b")
      assertThat(progress.isFinished).isTrue()
    }
  }

  @Test
  fun markAllFinished_whenTheBatchFails_leavesLocalProgressAlone() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", currentTime = 50f))
    api.batchUpdateMediaProgressResult = { Result.failure(IllegalStateException("offline")) }

    repository(db).markAllFinished(listOf(MediaProgressKey(BOOK_A), MediaProgressKey(BOOK_B)))

    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::isFinished).isFalse()
    assertThat(db.progressFor(BOOK_B)).isNull()
  }

  @Test
  fun markAllNotFinished_deletesEachItemsProgress() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true))
    db.insertProgress(progress(PODCAST, episodeId = EPISODE, id = "progress-ep", isFinished = true))
    api.deleteMediaProgressResult = { Result.success(Unit) }

    repository(db).markAllNotFinished(
      listOf(MediaProgressKey(BOOK_A), MediaProgressKey(PODCAST, EPISODE)),
    )

    assertThat(api.deletedMediaProgressIds).containsExactlyInAnyOrder("progress-a", "progress-ep")
    assertThat(db.progressFor(BOOK_A)).isNull()
    assertThat(db.progressFor(PODCAST, EPISODE)).isNull()
  }

  @Test
  fun markAllFinished_withNoItems_skipsTheServer() = testDb { db ->
    repository(db).markAllFinished(emptyList())

    assertThat(api.batchMediaProgressUpdates).isEmpty()
  }

  private fun playbackRow(id: String) = app.campfire.data.MediaProgress(
    id = id,
    libraryItemId = "item-1",
    userId = "user-1",
    episodeId = "",
    mediaItemId = "media-1",
    mediaItemType = MediaType.Book,
    duration = 3_600.0,
    progress = 0.01,
    currentTime = 36.0,
    isFinished = false,
    hideFromContinueListening = false,
    ebookLocation = null,
    ebookProgress = null,
    lastUpdate = 1_000L,
    startedAt = 1_000L,
    finishedAt = null,
    source = MediaProgress.Source.Local,
  )

  private suspend fun CampfireDatabase.insertProgress(progress: MediaProgress) {
    mediaProgressQueries.insert(progress.asDbModel())
  }

  private suspend fun CampfireDatabase.progressFor(
    libraryItemId: String,
    episodeId: String? = null,
  ): MediaProgress? {
    return mediaProgressQueries
      .selectForEpisode(USER_ID, libraryItemId, episodeId.orEmpty())
      .awaitAsOneOrNull()
      ?.asDomainModel()
  }

  private fun progress(
    libraryItemId: String,
    id: String,
    episodeId: String? = null,
    currentTime: Float = 0f,
    isFinished: Boolean = false,
  ) = MediaProgress(
    id = id,
    userId = USER_ID,
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    mediaItemId = episodeId ?: "media-$libraryItemId",
    mediaItemType = if (episodeId != null) MediaType.Podcast else MediaType.Book,
    duration = 100f,
    progress = if (isFinished) 1f else currentTime / 100f,
    currentTime = currentTime,
    isFinished = isFinished,
    hideFromContinueListening = false,
    lastUpdate = NOW - 1_000L,
    startedAt = 0L,
    source = MediaProgress.Source.Remote,
  )

  private fun networkProgress(
    libraryItemId: String,
    id: String,
    isFinished: Boolean,
  ) = NetworkMediaProgress(
    id = id,
    userId = USER_ID,
    libraryItemId = libraryItemId,
    mediaItemId = "media-$libraryItemId",
    mediaItemType = NetworkMediaType.Book,
    duration = 0f,
    progress = 1f,
    currentTime = 0f,
    isFinished = isFinished,
    hideFromContinueListening = false,
    lastUpdate = NOW,
    startedAt = NOW,
    finishedAt = NOW,
  )

  private object FixedFatherTime : FatherTime {
    override fun now(): LocalDateTime = error("not used in tests")
    override fun today(): LocalDate = error("not used in tests")
    override fun nowInEpochMillis(): Long = NOW
  }

  private companion object {
    const val USER_ID = "user-1"
    const val BOOK_A = "book-a"
    const val BOOK_B = "book-b"
    const val PODCAST = "podcast"
    const val EPISODE = "episode-1"
    const val NOW = 1_700_000_000_000L
  }
}
