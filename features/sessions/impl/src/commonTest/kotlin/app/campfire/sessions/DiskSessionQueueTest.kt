// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions

import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.session.UserSession
import app.campfire.data.SessionQueue as DbSessionQueue
import app.campfire.data.mapping.dao.SqlDelightLibraryItemDao
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.home.ui.libraryItem
import app.campfire.network.RequestOrigin
import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.MediaExpanded
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.user.test.fixtures.user
import app.cash.sqldelight.async.coroutines.awaitAsList
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.flow.first
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

  @Test
  fun `add by id queues the entry at the end`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)

    queue.add(podcast.id, "e1")

    assertThat(db.queueRows()).containsExactly(
      "book:" to 0,
      "podcast:e1" to 1,
    )
  }

  @Test
  fun `adding an already queued entry keeps its place`() = queueTest { db ->
    val queue = queue(db)
    queue.add(podcast, episode("e1"))
    queue.add(book)

    queue.add(podcast.id, "e1")

    assertThat(db.queueRows()).containsExactly(
      "podcast:e1" to 0,
      "book:" to 1,
    )
  }

  @Test
  fun `pop drops entries gone from the device and returns the next playable one`() = queueTest { db ->
    cacheBooks(db, "book")
    db.insertRows("gone" to "", "book" to "", "later" to "")

    val next = queue(db).pop()

    assertThat(next?.key).isEqualTo("book")
    assertThat(db.queueRows()).containsExactly("later:" to 0)
  }

  @Test
  fun `pop empties a queue with nothing left to play`() = queueTest { db ->
    db.insertRows("gone" to "")

    assertThat(queue(db).pop()).isNull()
    assertThat(db.queueRows()).isEmpty()
  }

  @Test
  fun `pop on an empty queue returns nothing`() = queueTest { db ->
    assertThat(queue(db).pop()).isNull()
  }

  @Test
  fun `an episode entry whose episode is gone is left out`() = queueTest { db ->
    cacheBooks(db, "book")
    db.insertRows("book" to "gone-episode", "book" to "")

    val shown = queue(db).observeAll().first()

    assertThat(shown.map { it.key }).containsExactly("book")
  }

  @Test
  fun `reorder saves the new order when some entries can't be shown`() = queueTest { db ->
    cacheBooks(db, "book", "book2")
    db.insertRows("book" to "", "gone" to "", "book2" to "")
    val queue = queue(db)
    val shown = queue.observeAll().first()

    queue.reorder(shown.reversed())

    assertThat(db.queueRows()).containsExactly(
      "book2:" to 0,
      "book:" to 1,
      "gone:" to 2,
    )
  }

  @Test
  fun `remove says where the entry was, and insert puts it back there`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)
    queue.add(podcast, episode("e1"))
    queue.add(podcast, episode("e2"))

    val index = queue.remove(podcast.id, "e1")
    assertThat(index).isEqualTo(1)
    assertThat(db.queueRows()).containsExactly(
      "book:" to 0,
      "podcast:e2" to 1,
    )

    queue.insert(podcast.id, "e1", index!!)
    assertThat(db.queueRows()).containsExactly(
      "book:" to 0,
      "podcast:e1" to 1,
      "podcast:e2" to 2,
    )
  }

  @Test
  fun `removing an entry that isn't queued says so`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)

    assertThat(queue.remove(podcast.id, "e1")).isNull()
    assertThat(db.queueRows()).containsExactly("book:" to 0)
  }

  @Test
  fun `insert past the end of the queue adds to the end`() = queueTest { db ->
    val queue = queue(db)
    queue.add(book)

    queue.insert(podcast.id, "e1", index = 5)

    assertThat(db.queueRows()).containsExactly(
      "book:" to 0,
      "podcast:e1" to 1,
    )
  }

  fun `add numbers new entries after the user's own queue only`() = queueTest { db ->
    db.sessionQueueQueries.insert(
      DbSessionQueue(userId = "other-user", libraryItemId = "theirs", episodeId = "", queueIndex = 5),
    )

    queue(db).add(book)

    assertThat(db.queueRows()).containsExactly("book:" to 0)
  }

  private fun TestScope.queue(db: CampfireDatabase): DiskSessionQueue {
    return DiskSessionQueue(
      userSession = userSession,
      db = db,
      libraryItemDao = dao(db),
      dispatcherProvider = asTestDispatcherProvider(),
    )
  }

  private val userSession = UserSession.LoggedIn(testUser)

  private fun TestScope.dao(db: CampfireDatabase) =
    SqlDelightLibraryItemDao(db, FakeUrlHydrator(), asTestDispatcherProvider(), userSession)

  private suspend fun TestScope.cacheBooks(db: CampfireDatabase, vararg ids: String) {
    ids.forEach { dao(db).insert(networkBook(it)) }
  }

  private suspend fun CampfireDatabase.insertRows(vararg rows: Pair<String, String>) {
    rows.forEachIndexed { index, (libraryItemId, episodeId) ->
      sessionQueueQueries.insert(
        DbSessionQueue(
          userId = testUser.id,
          libraryItemId = libraryItemId,
          episodeId = episodeId,
          queueIndex = index,
        ),
      )
    }
  }

  private fun networkBook(id: String) = LibraryItemExpanded.Book(
    id = id,
    ino = "ino-$id",
    libraryId = "library",
    folderId = "folder",
    path = "path/$id",
    relPath = id,
    isFile = false,
    mtimeMs = 0L,
    ctimeMs = 0L,
    birthtimeMs = 0L,
    addedAt = 0L,
    updatedAt = 0L,
    isMissing = false,
    isInvalid = false,
    mediaType = NetworkMediaType.Book,
    media = MediaExpanded(
      id = "media-$id",
      coverPath = null,
      metadata = ExpandedBookMetadata(title = id),
      audioFiles = emptyList(),
      tracks = emptyList(),
    ),
    libraryFiles = emptyList(),
  ).apply { applyOrigin(RequestOrigin.Url("server")) }

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
