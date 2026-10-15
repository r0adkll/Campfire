// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.mediaprogress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.session.UserSession
import app.campfire.core.time.FatherTime
import app.campfire.data.MediaProgressFailedUpdate
import app.campfire.db.test.testDb
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class DefaultEbookProgressUploaderTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )
  private val api = FakeAudioBookShelfApi()

  private fun uploader(db: CampfireDatabase) = DefaultEbookProgressUploader(
    userSession = UserSession.LoggedIn(user(id = USER_ID)),
    api = api,
    db = db,
    fatherTime = FixedFatherTime,
    dispatcherProvider = dispatchers,
    coroutineScopeHolder = CoroutineScopeHolder { CoroutineScope(Dispatchers.Default) },
  )

  @Test
  fun uploadPending_sendsOnlyTheEbookFields_andClearsTheMark() = testDb { db ->
    db.mediaProgressQueries.insert(row(id = "server-id"))
    db.markFailed(ITEM)
    api.updateMediaProgressResult = { Result.success(Unit) }

    uploader(db).uploadPending()

    assertThat(api.mediaProgressUpdates).containsExactly(
      MediaProgressUpdatePayload(ebookLocation = LOCATION, ebookProgress = 0.25f),
    )
    assertThat(db.failedItems()).isEmpty()
  }

  @Test
  fun uploadPending_keepsTheMarkWhenTheServerCantBeReached() = testDb { db ->
    db.mediaProgressQueries.insert(row(id = "server-id"))
    db.markFailed(ITEM)
    api.updateMediaProgressResult = { Result.failure(IllegalStateException("offline")) }

    uploader(db).uploadPending()

    assertThat(db.failedItems()).containsExactly(ITEM)
  }

  @Test
  fun uploadPending_adoptsTheIdTheServerIssued() = testDb { db ->
    db.mediaProgressQueries.insert(row(id = MediaProgress.UNKNOWN_ID))
    db.markFailed(ITEM)
    api.updateMediaProgressResult = { Result.success(Unit) }
    api.mediaProgressResult = { _, _ -> Result.success(networkProgress(id = "server-id")) }

    uploader(db).uploadPending()

    val stored = db.mediaProgressQueries.selectForEpisode(USER_ID, ITEM, "").awaitAsOne()
    assertThat(stored.id).isEqualTo("server-id")
    assertThat(stored.ebookLocation).isEqualTo(LOCATION)
  }

  private suspend fun CampfireDatabase.markFailed(libraryItemId: String) {
    mediaProgressBookkeepingQueries.insertFailedUpdateIfAbsent(
      MediaProgressFailedUpdate(libraryItemId = libraryItemId, userId = USER_ID, timestamp = NOW),
    )
  }

  private suspend fun CampfireDatabase.failedItems(): List<String> =
    mediaProgressBookkeepingQueries.getFailedUpdateItemsForUser(USER_ID).awaitAsList()

  private fun row(id: String) = app.campfire.data.MediaProgress(
    id = id,
    libraryItemId = ITEM,
    userId = USER_ID,
    episodeId = "",
    mediaItemId = "media-1",
    mediaItemType = MediaType.Book,
    duration = null,
    progress = 0.0,
    currentTime = 0.0,
    isFinished = false,
    hideFromContinueListening = false,
    ebookLocation = LOCATION,
    ebookProgress = 0.25,
    lastUpdate = NOW,
    startedAt = NOW,
    finishedAt = null,
    source = MediaProgress.Source.Local,
  )

  private fun networkProgress(id: String) = NetworkMediaProgress(
    id = id,
    userId = USER_ID,
    libraryItemId = ITEM,
    mediaItemId = "media-1",
    mediaItemType = NetworkMediaType.Book,
    duration = 0f,
    progress = 0f,
    currentTime = 0f,
    isFinished = false,
    hideFromContinueListening = false,
    ebookLocation = LOCATION,
    ebookProgress = 0.25f,
    lastUpdate = NOW,
    startedAt = NOW,
  )

  private object FixedFatherTime : FatherTime {
    override fun now(): LocalDateTime = error("not used in tests")
    override fun today(): LocalDate = error("not used in tests")
    override fun nowInEpochMillis(): Long = NOW
  }

  private companion object {
    const val USER_ID = "user-1"
    const val ITEM = "item-1"
    const val LOCATION = "epubcfi(/6/4!/4/2/1:0)"
    const val NOW = 1_700_000_000_000L
  }
}
