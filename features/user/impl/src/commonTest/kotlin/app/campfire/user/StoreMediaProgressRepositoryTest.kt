// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.core.session.UserSession
import app.campfire.core.time.GrandFatherTime
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.test.testDb
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.mediaprogress.MediaProgressSynchronizer
import app.campfire.user.mediaprogress.store.MediaProgressStore
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsOne
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class StoreMediaProgressRepositoryTest {

  private val session = UserSession.LoggedIn(user(id = "user-1"))
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
    fatherTime = GrandFatherTime,
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
}
