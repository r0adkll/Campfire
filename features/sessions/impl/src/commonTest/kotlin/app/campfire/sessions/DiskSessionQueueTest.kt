// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.home.ui.libraryItem
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DiskSessionQueueTest {

  private val testUser = user("user")
  private val book = libraryItem(id = "book")
  private val podcast = libraryItem(id = "podcast", mediaType = MediaType.Podcast)

  @Test
  fun `addFirst puts the entry ahead of everything already queued`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)
    queue.add(podcast, episode("e2"))

    queue.addFirst(podcast, episode("e1"))

    assertThat(db.queueRows()).containsExactly(
      "podcast:e1" to 0,
      "book:" to 1,
      "podcast:e2" to 2,
    )
  }

  @Test
  fun `addFirst moves an already queued entry to the front`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)
    queue.add(podcast, episode("e1"))

    queue.addFirst(podcast, episode("e1"))

    assertThat(db.queueRows()).containsExactly(
      "podcast:e1" to 0,
      "book:" to 1,
    )
  }

  private fun TestScope.queue(db: CampfireDatabase): DiskSessionQueue {
    val dispatcherProvider = asTestDispatcherProvider()
    val userSession = UserSession.LoggedIn(testUser)
    return DiskSessionQueue(
      userSession = userSession,
      db = db,
      libraryItemDao = SqlDelightLibraryItemDao(db, FakeUrlHydrator(), dispatcherProvider, userSession),
      dispatcherProvider = dispatcherProvider,
    )
  }

  private suspend fun CampfireDatabase.queueRows(): List<Pair<String, Int>> {
    return sessionQueueQueries.selectAll(testUser.id)
      .awaitAsList()
      .map { "${it.libraryItemId}:${it.episodeId}" to it.queueIndex }
  }

  private fun episode(id: String) = PodcastEpisode(
    id = id,
    libraryItemId = podcast.id,
    podcastId = "podcast-media",
    title = "Episode $id",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )

  private fun queueTest(block: suspend TestScope.(CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }
}
