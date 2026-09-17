// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.purge

import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager.Invocation.DeleteAllForItemId
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.network.ApiException
import app.campfire.network.test.FakeAudioBookShelfApi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import kotlin.test.Test

class DefaultLibraryItemPurgerTest {

  private val api = FakeAudioBookShelfApi()
  private val downloads = FakeOfflineDownloadManager()

  private fun kotlinx.coroutines.test.TestScope.purger(database: PurgeTestDatabase) = DefaultLibraryItemPurger(
    db = database.db,
    api = api,
    offlineDownloadManager = downloads,
    dispatcherProvider = asTestDispatcherProvider(),
  )

  @Test
  fun `purge removes an item and every reference to it with foreign keys enforced`() = purgeTest { database ->
    database.book("removed")
    database.book("kept")
    database.linkEverywhere("removed")
    database.linkEverywhere("kept")
    database.enforceForeignKeys()

    purger(database).purge(listOf("removed"))

    assertThat(database.ids("libraryItem", column = "id")).containsExactly("kept")
    assertThat(database.ids("media")).containsExactly("kept")
    assertThat(database.ids("mediaChapters", column = "mediaId")).containsExactly("media-kept")
    assertThat(database.ids("seriesBookJoin")).containsExactly("kept")
    assertThat(database.ids("collectionsBookJoin")).containsExactly("kept")
    assertThat(database.ids("playlistItemJoin")).containsExactly("kept")
    assertThat(database.ids("libraryItemPageJoin")).containsExactly("kept")
    assertThat(database.ids("shelfJoin", column = "entityId")).containsExactly("kept")
    assertThat(database.ids("search_books")).containsExactly("kept")
    assertThat(downloads.invocations).containsExactly(DeleteAllForItemId("removed"))
  }

  @Test
  fun `purgeIfRemoved only purges items the server reports as not found`() = purgeTest { database ->
    database.book("gone")
    database.book("forbidden")
    database.book("erroring")
    api.libraryItemResult = { id ->
      when (id) {
        "gone" -> Result.failure(ApiException(404))
        "forbidden" -> Result.failure(ApiException(403))
        else -> Result.failure(ApiException(500))
      }
    }

    val removed = purger(database).purgeIfRemoved(listOf("gone", "forbidden", "erroring"))

    assertThat(removed).containsOnly("gone")
    assertThat(database.ids("libraryItem", column = "id")).containsExactly("erroring", "forbidden")
  }

  @Test
  fun `purgeIfRemoved stops asking once the server is unreachable`() = purgeTest { database ->
    database.book("unreachable")
    database.book("gone")
    api.libraryItemResult = { id ->
      if (id == "gone") Result.failure(ApiException(404)) else Result.failure(RuntimeException("offline"))
    }

    val removed = purger(database).purgeIfRemoved(listOf("unreachable", "gone"))

    assertThat(removed).isEmpty()
    assertThat(api.libraryItemRequests).containsExactly("unreachable")
    assertThat(database.ids("libraryItem", column = "id")).containsExactly("gone", "unreachable")
  }
}
