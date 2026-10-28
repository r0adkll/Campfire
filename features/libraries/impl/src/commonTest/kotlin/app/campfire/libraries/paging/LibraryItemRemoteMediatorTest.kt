// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.RemoteMediator.InitializeAction
import app.campfire.CampfireDatabase
import app.campfire.account.test.FakeUrlHydrator
import app.campfire.common.test.coroutines.asTestDispatcherProvider
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.campfire.network.test.FakeAudioBookShelfApi
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPagingApi::class)
class LibraryItemRemoteMediatorTest {

  private val api = FakeAudioBookShelfApi()

  @Test
  fun `a podcast library refreshes every time it opens`() = mediatorTest { db, mediator ->
    db.library(mediaType = "podcast")
    api.libraryItemsMinifiedResult = { _, _ -> Result.success(page(podcast("pod_1", numEpisodesIncomplete = 2))) }
    mediator.load(LoadType.REFRESH, emptyState())

    assertThat(mediator.initialize()).isEqualTo(InitializeAction.LAUNCH_INITIAL_REFRESH)
  }

  @Test
  fun `a podcast library's pages skip the server's response cache`() = mediatorTest { db, mediator ->
    db.library(mediaType = "podcast")
    api.libraryItemsMinifiedResult = { _, _ -> Result.success(page(podcast("pod_1", numEpisodesIncomplete = 2))) }

    mediator.load(LoadType.REFRESH, emptyState())

    assertThat(api.libraryItemsMinifiedSkippedServerCache).containsExactly(true)
  }

  @Test
  fun `a book library's pages can come from the server's response cache`() = mediatorTest { db, mediator ->
    db.library(mediaType = "book")
    api.libraryItemsMinifiedResult = { _, _ -> Result.success(page(book("book_1"))) }

    mediator.load(LoadType.REFRESH, emptyState())

    assertThat(api.libraryItemsMinifiedSkippedServerCache).containsExactly(false)
  }

  @Test
  fun `a book library keeps its pages for a day`() = mediatorTest { db, mediator ->
    db.library(mediaType = "book")
    api.libraryItemsMinifiedResult = { _, _ -> Result.success(page(book("book_1"))) }
    mediator.load(LoadType.REFRESH, emptyState())

    assertThat(mediator.initialize()).isEqualTo(InitializeAction.SKIP_INITIAL_REFRESH)
  }

  private fun mediatorTest(
    block: suspend TestScope.(CampfireDatabase, LibraryItemRemoteMediator) -> Unit,
  ) = runTest {
    val driver = createDriver()
    try {
      val db = DatabaseFactory(driver).build()
      val mediator = LibraryItemRemoteMediator(
        user = TestUser,
        input = TestInput,
        api = api,
        db = db,
        dispatcherProvider = asTestDispatcherProvider(),
        fatherTime = FixedFatherTime(LOADED_AT),
        urlHydrator = FakeUrlHydrator(),
      )
      block(db, mediator)
    } finally {
      driver.close()
    }
  }
}
