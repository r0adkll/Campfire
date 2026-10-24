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
import app.campfire.user.api.StartedAtNotChanged
import app.campfire.user.mediaprogress.EbookProgressUploader
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
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

  private val ebookUploads = RecordingEbookProgressUploader()

  private fun repository(db: CampfireDatabase) = StoreMediaProgressRepository(
    userSession = session,
    storeFactory = MediaProgressStore.Factory(api, db, dispatchers),
    db = db,
    api = api,
    libraryItemDao = SqlDelightLibraryItemDao(db, FakeUrlHydrator(), dispatchers, session),
    mediaProgressSynchronizer = object : MediaProgressSynchronizer {
      override suspend fun sync(mediaProgress: MediaProgress, force: Boolean) = Unit
    },
    ebookProgressUploader = ebookUploads,
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

  @Test
  fun setFinishedAt_changesTheRowAndSendsOnlyTheFinishFields() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true, finishedAt = NOW))

    val result = repository(db).setFinishedAt(BOOK_A, finishedAt = EARLIER)

    assertThat(result.isSuccess).isTrue()
    assertThat(db.progressFor(BOOK_A)).isNotNull().given { progress ->
      assertThat(progress.finishedAt).isEqualTo(EARLIER)
      assertThat(progress.isFinished).isTrue()
      assertThat(progress.id).isEqualTo("progress-a")
    }
    assertThat(api.mediaProgressUpdates).containsExactly(
      MediaProgressUpdatePayload(isFinished = true, finishedAt = EARLIER),
    )
  }

  @Test
  fun setFinishedAt_ofAnEpisode_changesOnlyThatEpisode() = testDb { db ->
    db.insertProgress(progress(PODCAST, episodeId = EPISODE, id = "progress-ep", isFinished = true, finishedAt = NOW))

    repository(db).setFinishedAt(PODCAST, finishedAt = EARLIER, episodeId = EPISODE)

    assertThat(db.progressFor(PODCAST, EPISODE)).isNotNull().prop(MediaProgress::finishedAt).isEqualTo(EARLIER)
    assertThat(api.mediaProgressUpdates).containsExactly(
      MediaProgressUpdatePayload(episodeId = EPISODE, isFinished = true, finishedAt = EARLIER),
    )
  }

  @Test
  fun setFinishedAt_whenTheServerRefuses_putsTheOldDateBack() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true, finishedAt = NOW))
    api.updateMediaProgressResult = { Result.failure(IllegalStateException("offline")) }

    val result = repository(db).setFinishedAt(BOOK_A, finishedAt = EARLIER)

    assertThat(result.isFailure).isTrue()
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::finishedAt).isEqualTo(NOW)
  }

  @Test
  fun setFinishedAt_withoutFinishedProgress_leavesTheServerAlone() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", currentTime = 50f))

    val unfinished = repository(db).setFinishedAt(BOOK_A, finishedAt = EARLIER)
    val missing = repository(db).setFinishedAt(BOOK_B, finishedAt = EARLIER)

    assertThat(unfinished.isFailure).isTrue()
    assertThat(missing.isFailure).isTrue()
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::finishedAt).isNull()
    assertThat(api.mediaProgressUpdates).isEmpty()
  }

  @Test
  fun setStartedAt_createsTheProgressAgainFromTheServersCopy() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true, finishedAt = NOW))
    val original = startedProgress(id = "progress-a", startedAt = EARLIER)
    val serverCopies = mutableListOf(original, original.copy(id = "progress-a2", startedAt = EARLIEST))
    api.mediaProgressResult = { _, _ -> Result.success(serverCopies.removeFirst()) }
    api.deleteMediaProgressResult = { Result.success(Unit) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.isSuccess).isTrue()
    assertThat(api.deletedMediaProgressIds).containsExactly("progress-a")
    assertThat(api.mediaProgressUpdates).containsExactly(original.creation(createdAt = EARLIEST))
    assertThat(db.progressFor(BOOK_A)).isNotNull().given { progress ->
      assertThat(progress.id).isEqualTo("progress-a2")
      assertThat(progress.startedAt).isEqualTo(EARLIEST)
      assertThat(progress.isFinished).isTrue()
      assertThat(progress.finishedAt).isEqualTo(NOW)
    }
  }

  @Test
  fun setStartedAt_whenTheServerCantBeReached_changesNothing() = testDb { db ->
    api.mediaProgressResult = { _, _ -> Result.failure(IllegalStateException("offline")) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<StartedAtNotChanged.Unchanged>()
    assertThat(api.deletedMediaProgressIds).isEmpty()
    assertThat(api.mediaProgressUpdates).isEmpty()
  }

  @Test
  fun setStartedAt_whenTheDeleteIsRefused_createsNothing() = testDb { db ->
    api.mediaProgressResult = { _, _ -> Result.success(startedProgress(id = "progress-a", startedAt = EARLIER)) }
    api.deleteMediaProgressResult = { Result.failure(IllegalStateException("offline")) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<StartedAtNotChanged.Unchanged>()
    assertThat(api.mediaProgressUpdates).isEmpty()
  }

  @Test
  fun setStartedAt_whenCreatingAgainFails_putsTheOriginalBack() = testDb { db ->
    val original = startedProgress(id = "progress-a", startedAt = EARLIER)
    api.mediaProgressResult = { _, _ -> Result.success(original) }
    api.deleteMediaProgressResult = { Result.success(Unit) }
    var creates = 0
    api.updateMediaProgressResult = {
      if (creates++ == 0) Result.failure(IllegalStateException("bad request")) else Result.success(Unit)
    }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<StartedAtNotChanged.Restored>()
    assertThat(api.mediaProgressUpdates).containsExactly(
      original.creation(createdAt = EARLIEST),
      original.creation(createdAt = EARLIER),
    )
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::startedAt).isEqualTo(EARLIER)
  }

  @Test
  fun setStartedAt_whenTheOriginalCantBePutBack_keepsItHereToRestoreLater() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true, finishedAt = NOW, startedAt = EARLIER))
    api.mediaProgressResult = { _, _ -> Result.success(startedProgress(id = "progress-a", startedAt = EARLIER)) }
    api.deleteMediaProgressResult = { Result.success(Unit) }
    api.updateMediaProgressResult = { Result.failure(IllegalStateException("offline")) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<StartedAtNotChanged.NotRestored>()
    // One create, then three tries at putting it back
    assertThat(api.mediaProgressUpdates).hasSize(4)
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::id).isEqualTo("progress-a")

    // Back online, this device's copy goes back up
    api.mediaProgressUpdates.clear()
    api.updateMediaProgressResult = { Result.success(Unit) }
    api.mediaProgressResult = { _, _ -> Result.success(startedProgress(id = "progress-a3", startedAt = EARLIER)) }

    val restored = repository(db).restoreProgress(BOOK_A)

    assertThat(restored.isSuccess).isTrue()
    assertThat(api.mediaProgressUpdates).single().given { creation ->
      assertThat(creation.createdAt).isEqualTo(EARLIER)
      assertThat(creation.isFinished).isEqualTo(true)
      assertThat(creation.finishedAt).isEqualTo(NOW)
    }
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::id).isEqualTo("progress-a3")
  }

  @Test
  fun setStartedAt_whenTheServerKeepsItsOwnStart_saysSo() = testDb { db ->
    val original = startedProgress(id = "progress-a", startedAt = EARLIER)
    val serverCopies = mutableListOf(original, original.copy(id = "progress-a2", startedAt = NOW))
    api.mediaProgressResult = { _, _ -> Result.success(serverCopies.removeFirst()) }
    api.deleteMediaProgressResult = { Result.success(Unit) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.exceptionOrNull()).isNotNull().isInstanceOf<StartedAtNotChanged.ServerKept>()
    assertThat(db.progressFor(BOOK_A)).isNotNull().prop(MediaProgress::id).isEqualTo("progress-a2")
  }

  @Test
  fun setStartedAt_whenTheNewProgressCantBeFetched_holdsTheNewStartUntilItIs() = testDb { db ->
    db.insertProgress(progress(BOOK_A, id = "progress-a", isFinished = true, finishedAt = NOW, startedAt = EARLIER))
    val serverCopies = mutableListOf(Result.success(startedProgress(id = "progress-a", startedAt = EARLIER)))
    api.mediaProgressResult = { _, _ ->
      serverCopies.removeFirstOrNull() ?: Result.failure(IllegalStateException("offline"))
    }
    api.deleteMediaProgressResult = { Result.success(Unit) }

    val result = repository(db).setStartedAt(BOOK_A, startedAt = EARLIEST)

    assertThat(result.isSuccess).isTrue()
    assertThat(db.progressFor(BOOK_A)).isNotNull().given { progress ->
      assertThat(progress.id).isEqualTo(MediaProgress.UNKNOWN_ID)
      assertThat(progress.startedAt).isEqualTo(EARLIEST)
    }
  }

  @Test
  fun updateEbookProgress_keepsTheListeningProgressOfAnExistingRow() = testDb { db ->
    db.mediaProgressQueries.insert(playbackRow(id = "server-progress-id"))

    repository(db).updateEbookProgress("item-1", location = "epubcfi(/6/4!/4/2/1:0)", progress = 0.25f)

    val row = db.mediaProgressQueries.selectForEpisode("user-1", "item-1", "").awaitAsOne()
    assertThat(row.ebookLocation).isEqualTo("epubcfi(/6/4!/4/2/1:0)")
    assertThat(row.ebookProgress).isEqualTo(0.25)
    assertThat(row.currentTime).isEqualTo(36.0)
    assertThat(row.id).isEqualTo("server-progress-id")
    assertThat(row.lastUpdate).isEqualTo(NOW)
    assertThat(ebookUploads.requested).containsExactly("item-1")
  }

  @Test
  fun updateEbookProgress_withoutAnItemToStartFrom_writesNothing() = testDb { db ->
    repository(db).updateEbookProgress("missing", location = "3", progress = 0.5f)

    assertThat(db.mediaProgressQueries.selectForEpisode("user-1", "missing", "").awaitAsOneOrNull()).isNull()
    assertThat(ebookUploads.requested).isEmpty()
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
    finishedAt: Long? = null,
    startedAt: Long = 0L,
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
    startedAt = startedAt,
    finishedAt = finishedAt,
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

  /** A finished book's progress as the server has it, read in an ebook too so every field carries over. */
  private fun startedProgress(id: String, startedAt: Long) = NetworkMediaProgress(
    id = id,
    userId = USER_ID,
    libraryItemId = BOOK_A,
    mediaItemId = "media-$BOOK_A",
    mediaItemType = NetworkMediaType.Book,
    duration = 3_600f,
    progress = 1f,
    currentTime = 3_590f,
    isFinished = true,
    hideFromContinueListening = true,
    ebookLocation = "epubcfi(/6/4!/4/2/1:0)",
    ebookProgress = 0.4f,
    lastUpdate = NOW,
    startedAt = startedAt,
    finishedAt = NOW,
  )

  private fun NetworkMediaProgress.creation(createdAt: Long) = MediaProgressUpdatePayload(
    duration = duration,
    progress = progress,
    currentTime = currentTime,
    isFinished = isFinished,
    hideFromContinueListening = hideFromContinueListening,
    finishedAt = finishedAt,
    ebookLocation = ebookLocation,
    ebookProgress = ebookProgress,
    createdAt = createdAt,
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
    const val EARLIER = NOW - 86_400_000L
    const val EARLIEST = NOW - 10 * 86_400_000L
  }
}

private class RecordingEbookProgressUploader : EbookProgressUploader {
  val requested = mutableListOf<String>()

  override fun request(libraryItemId: String) {
    requested += libraryItemId
  }

  override suspend fun uploadPending() = Unit
}
