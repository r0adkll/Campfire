// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.purge

import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.libraries.api.LibraryItemPurger
import app.campfire.network.PagedResponse
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaMinified
import app.campfire.network.models.MediaType
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.test.FakeAudioBookShelfApi
import app.campfire.user.test.FakeUserRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class RemovedLibraryItemReconcilerTest {

  private val api = FakeAudioBookShelfApi()
  private val purger = RecordingPurger()
  private val settings = MapSettings()
  private val fatherTime = MutableFatherTime()

  private fun TestScope.reconciler(database: PurgeTestDatabase) = RemovedLibraryItemReconciler(
    api = api,
    db = database.db,
    purger = purger,
    userRepository = FakeUserRepository(),
    settings = settings,
    fatherTime = fatherTime,
    dispatcherProvider = asTestDispatcherProvider(),
    coroutineScopeHolder = CoroutineScopeHolder { this },
  )

  @Test
  fun `asks about cached items in the library that the server no longer lists`() = purgeTest { database ->
    database.book("listed")
    database.book("missing")
    database.book("other-library", libraryId = "other")
    api.libraryItemsMinifiedResult = { _, page -> Result.success(page(page, listOf("listed"), total = 1)) }

    val reconciled = reconciler(database).reconcile(PurgeTestDatabase.LIBRARY_ID)

    assertThat(reconciled).isTrue()
    assertThat(purger.checked).containsExactly(listOf("missing"))
  }

  @Test
  fun `pages through the whole listing before comparing`() = purgeTest { database ->
    database.book("first")
    database.book("second")
    api.libraryItemsMinifiedResult = { _, page ->
      Result.success(page(page, listOf(if (page == 0) "first" else "second"), total = 2))
    }

    reconciler(database).reconcile(PurgeTestDatabase.LIBRARY_ID)

    assertThat(api.libraryItemsMinifiedRequests).hasSize(2)
    assertThat(purger.checked).isEmpty()
  }

  @Test
  fun `skips the pass when the listing comes back short of the total`() = purgeTest { database ->
    database.book("listed")
    database.book("missing")
    // Claims two items but ends after one, e.g. an item removed mid-listing shifting the pages.
    api.libraryItemsMinifiedResult = { _, page ->
      Result.success(PagedResponse(listOf(book("listed")), page = page, limit = 1, total = 2, offset = 1))
    }

    val reconciled = reconciler(database).reconcile(PurgeTestDatabase.LIBRARY_ID)

    assertThat(reconciled).isFalse()
    assertThat(purger.checked).isEmpty()
  }

  @Test
  fun `skips the pass when the listing fails`() = purgeTest { database ->
    database.book("cached")
    api.libraryItemsMinifiedResult = { _, _ -> Result.failure(RuntimeException("offline")) }

    val reconciled = reconciler(database).reconcile(PurgeTestDatabase.LIBRARY_ID)

    assertThat(reconciled).isFalse()
    assertThat(purger.checked).isEmpty()
  }

  @Test
  fun `checks at most fifty candidates per pass`() = purgeTest { database ->
    repeat(60) { database.book("missing-$it") }
    api.libraryItemsMinifiedResult = { _, page -> Result.success(page(page, emptyList(), total = 0)) }

    reconciler(database).reconcile(PurgeTestDatabase.LIBRARY_ID)

    assertThat(purger.checked.single()).hasSize(50)
  }

  @Test
  fun `runs at most once a day per library`() = purgeTest { database ->
    api.libraryItemsMinifiedResult = { _, page -> Result.success(page(page, emptyList(), total = 0)) }
    val reconciler = reconciler(database)

    reconciler.reconcileIfStale(PurgeTestDatabase.LIBRARY_ID)
    fatherTime.nowMillis += 23.hours.inWholeMilliseconds
    reconciler.reconcileIfStale(PurgeTestDatabase.LIBRARY_ID)
    reconciler.reconcileIfStale("other")

    assertThat(api.libraryItemsMinifiedRequests).containsExactlyInAnyOrder(PurgeTestDatabase.LIBRARY_ID, "other")

    fatherTime.nowMillis += 1.hours.inWholeMilliseconds
    reconciler.reconcileIfStale(PurgeTestDatabase.LIBRARY_ID)

    assertThat(api.libraryItemsMinifiedRequests).hasSize(3)
  }

  @Test
  fun `retries the next time when a pass did not complete`() = purgeTest { database ->
    api.libraryItemsMinifiedResult = { _, _ -> Result.failure(RuntimeException("offline")) }
    val reconciler = reconciler(database)

    reconciler.reconcileIfStale(PurgeTestDatabase.LIBRARY_ID)
    reconciler.reconcileIfStale(PurgeTestDatabase.LIBRARY_ID)

    assertThat(api.libraryItemsMinifiedRequests).hasSize(2)
  }

  private fun page(page: Int, ids: List<String>, total: Int) = PagedResponse(
    data = ids.map(::book),
    page = page,
    limit = 1,
    total = total,
    offset = page,
  )

  private fun book(id: String): LibraryItemMinified = LibraryItemMinified.Book(
    id = id,
    ino = "ino",
    libraryId = PurgeTestDatabase.LIBRARY_ID,
    folderId = "folder",
    path = "path",
    relPath = "relPath",
    isFile = false,
    mtimeMs = 0,
    ctimeMs = 0,
    birthtimeMs = 0,
    addedAt = 0,
    updatedAt = 0,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Book,
    size = 0,
    media = MediaMinified(id = "media-$id", coverPath = null, metadata = MinifiedBookMetadata()),
  )
}

private class RecordingPurger : LibraryItemPurger {
  val checked = mutableListOf<List<LibraryItemId>>()

  override suspend fun purge(itemIds: Collection<LibraryItemId>) = Unit

  override suspend fun purgeIfRemoved(candidates: Collection<LibraryItemId>): Set<LibraryItemId> {
    checked += candidates.toList()
    return emptySet()
  }
}

private class MutableFatherTime(var nowMillis: Long = 1_000_000_000L) : FatherTime {
  override fun now(): LocalDateTime = error("not used in tests")
  override fun today(): LocalDate = error("not used in tests")
  override fun nowInEpochMillis(): Long = nowMillis
}
