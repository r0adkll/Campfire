// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.covers.impl

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.LibraryItemRepository
import app.campfire.libraries.api.RescanResult
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class OfflineCoverKeeperTest {

  private val root = tempCoverDirectory()
  private val covers = FileOfflineCovers(storage = { root.toString() }, dispatcherProvider = testDispatchers)
  private val downloads = FakeDownloads()
  private val items = MapLibraryItemRepository()

  private var serverUp = true
  private val requests = mutableListOf<String>()
  private val engine = MockEngine { request ->
    requests += request.url.toString()
    if (serverUp) respond(COVER, HttpStatusCode.OK) else respondError(HttpStatusCode.ServiceUnavailable)
  }

  private fun keeper(scope: CoroutineScope = CoroutineScope(testDispatchers.io)) = OfflineCoverKeeper(
    downloads = downloads,
    libraryItemRepository = items,
    covers = covers,
    client = HttpClient(engine),
    coroutineScopeHolder = CoroutineScopeHolder { scope },
  )

  @AfterTest
  fun cleanUp() = root.deleteRecursively()

  @Test
  fun downloadedItems_keepTheirCover_fetchedLargeFromTheServer() = runTest {
    items += item("li_1", ts = 5)

    keeper().reconcile(setOf("li_1"))

    assertThat(requests).containsExactly("$SERVER/api/items/li_1/cover?ts=5&width=1200")
    assertThat(covers.get("li_1")?.timestamp).isEqualTo(5L)
  }

  @Test
  fun aCurrentCover_isntFetchedAgain() = runTest {
    items += item("li_1", ts = 5)
    keeper().reconcile(setOf("li_1"))

    keeper().reconcile(setOf("li_1"))

    assertThat(requests.size).isEqualTo(1)
  }

  @Test
  fun anUpdatedItem_refreshesItsCover() = runTest {
    items += item("li_1", ts = 5)
    keeper().reconcile(setOf("li_1"))

    items += item("li_1", ts = 6)
    keeper().reconcile(setOf("li_1"))

    assertThat(requests.size).isEqualTo(2)
    assertThat(covers.get("li_1")?.timestamp).isEqualTo(6L)
  }

  @Test
  fun coverIsLetGo_onceNothingOfTheItemIsDownloaded() = runTest {
    items += item("li_1", ts = 5)
    items += item("li_2", ts = 5)
    keeper().reconcile(setOf("li_1", "li_2"))

    keeper().reconcile(setOf("li_2"))

    assertThat(covers.itemIds()).containsOnly("li_2")
  }

  @Test
  fun offline_theKeptCoverStays_andOthersAreTriedLater() = runTest {
    items += item("li_1", ts = 5)
    items += item("li_2", ts = 5)
    keeper().reconcile(setOf("li_1"))
    serverUp = false
    items += item("li_1", ts = 6)

    keeper().reconcile(setOf("li_1", "li_2"))

    assertThat(covers.get("li_1")?.timestamp).isEqualTo(5L)
    assertThat(covers.itemIds()).containsOnly("li_1")
  }

  @Test
  fun itemsWithoutACover_andUnknownItems_areSkipped() = runTest {
    items += item("li_1", ts = 5, coverPath = null)

    keeper().reconcile(setOf("li_1", "li_missing"))

    assertThat(requests).isEmpty()
    assertThat(covers.itemIds()).isEmpty()
  }

  @Test
  fun everyItemWithADownload_isFollowed() = runTest {
    items += item("li_audio", ts = 1)
    items += item("li_ebook", ts = 1)
    items += item("li_episode", ts = 1)
    val scope = CoroutineScope(testDispatchers.io)
    keeper(scope).onCreate()

    downloads.downloads.value = listOf(
      Download(DownloadContent.Audiobook("li_audio"), Download.State.Downloaded),
      Download(DownloadContent.Ebook("li_ebook"), Download.State.Downloading),
      Download(DownloadContent.Episode("li_episode", "ep_1"), Download.State.Queued),
    )
    awaitKept("li_audio", "li_ebook", "li_episode")

    downloads.downloads.value = downloads.downloads.value.filter { it.content.itemId != "li_ebook" }
    awaitKept("li_audio", "li_episode")
    scope.cancel()
  }

  private suspend fun awaitKept(vararg itemIds: LibraryItemId) = withContext(testDispatchers.io) {
    withTimeoutOrNull(5.seconds) {
      while (covers.itemIds() != itemIds.toSet()) delay(10.milliseconds)
    }
    assertThat(covers.itemIds()).containsExactlyInAnyOrder(*itemIds)
  }

  private fun item(id: LibraryItemId, ts: Long, coverPath: String? = "/covers/$id.jpg") = libraryItem(
    id = id,
    media = media(coverImageUrl = "$SERVER/api/items/$id/cover?ts=$ts", coverPath = coverPath),
  )

  private class MapLibraryItemRepository : LibraryItemRepository {
    private val items = mutableMapOf<LibraryItemId, LibraryItem>()

    operator fun plusAssign(item: LibraryItem) {
      items[item.id] = item
    }

    override fun observeLibraryItem(itemId: LibraryItemId): Flow<LibraryItem> = emptyFlow()

    override suspend fun getLibraryItem(itemId: LibraryItemId): LibraryItem =
      items[itemId] ?: throw NoSuchElementException(itemId)

    override suspend fun deleteLibraryItem(itemId: LibraryItemId, hardDelete: Boolean) = Result.success(Unit)
    override suspend fun rescanLibraryItem(itemId: LibraryItemId) = Result.success(RescanResult.UpToDate)
    override suspend fun updateBookDetails(itemId: LibraryItemId, update: BookDetailsUpdate) = Result.success(false)
  }

  private companion object {
    const val SERVER = "https://abs.example.com"
    val COVER = byteArrayOf(1, 2, 3)
  }
}
