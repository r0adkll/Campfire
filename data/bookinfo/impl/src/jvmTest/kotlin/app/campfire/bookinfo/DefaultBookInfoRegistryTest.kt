// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.CommunitySource
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.db.BookInfoDatabase
import app.campfire.bookinfo.store.BookInfoStore
import app.campfire.bookinfo.test.FakeBookInfoProvider
import app.campfire.common.test.coroutines.TestDispatcherProvider
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.session.UserSession
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DefaultBookInfoRegistryTest {

  private val session: UserSession = UserSession.LoggedIn(user(id = "user-1"))
  private val item = libraryItem()

  private val communityInfo = BookCommunityInfo(
    providerBookId = "386446",
    providerUrl = "https://hardcover.app/books/the-way-of-kings",
    rating = 4.63,
    ratingsCount = 4109,
    ratingsDistribution = mapOf(5 to 3000),
    reviewsCount = 422,
    releaseDate = "2010-08-31",
    coverUrl = null,
  )

  private fun TestScope.registry(
    provider: FakeBookInfoProvider,
    settings: DefaultBookInfoProviderSettings = DefaultBookInfoProviderSettings(MapSettings(), session),
  ): DefaultBookInfoRegistry {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    BookInfoDatabase.Schema.synchronous().create(driver)
    val store = BookInfoStore(
      providers = setOf(provider),
      db = BookInfoDatabase(driver),
      dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
    )
    return DefaultBookInfoRegistry(
      providers = setOf(provider),
      settings = settings,
      store = store,
      userSession = session,
    )
  }

  @Test
  fun `providers are reported with enablement and link state`() = runTest {
    val provider = FakeBookInfoProvider()
    val registry = registry(provider)

    val statuses = registry.observeProviders().first()

    assertThat(statuses.size).isEqualTo(1)
    assertThat(statuses.single().enabled).isTrue()
    assertThat(statuses.single().linkState).isEqualTo(ProviderLinkState.Linked(null))
  }

  @Test
  fun `community info is loaded from a linked provider with attribution`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    val loaded = (state as LoadState.Loaded).data
    assertThat(loaded).isNotNull()
    assertThat(loaded!!.providerId).isEqualTo(ProviderId.Hardcover)
    assertThat(loaded.providerName).isEqualTo("Fake Provider")
    assertThat(loaded.info.rating).isEqualTo(4.63)
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `a fresh cache is served without refetching`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }
    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `an unlinked provider yields no community info`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.linkState.value = ProviderLinkState.NotLinked
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data).isNull()
  }

  @Test
  fun `a disabled provider yields no community info`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setEnabled(ProviderId.Hardcover, false)
    val registry = registry(provider, settings)

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data).isNull()
    assertThat(provider.bookInfoRequests.size).isEqualTo(0)
  }

  @Test
  fun `an invalid link serves cached data with a relink flag and no refetch`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }
    provider.linkState.value = ProviderLinkState.Invalid

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    val loaded = (state as LoadState.Loaded).data
    assertThat(loaded).isNotNull()
    assertThat(loaded!!.needsRelink).isTrue()
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `community info carries the available sources`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    val loaded = (state as LoadState.Loaded).data
    assertThat(loaded!!.availableSources)
      .isEqualTo(listOf(CommunitySource(ProviderId.Hardcover, "Fake Provider")))
  }

  @Test
  fun `a preferred provider is used when it can serve`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry
      .observeCommunityInfo(item, preferredProvider = ProviderId.Hardcover)
      .first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data!!.providerId).isEqualTo(ProviderId.Hardcover)
  }

  @Test
  fun `an unusable preferred provider falls back to the automatic pick`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry
      .observeCommunityInfo(item, preferredProvider = ProviderId.OpenLibrary)
      .first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data!!.providerId).isEqualTo(ProviderId.Hardcover)
  }

  @Test
  fun `changed item identifiers invalidate a fresh cache`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)
    val asinOnly = libraryItem(
      id = item.id,
      media = media(metadata = mediaMetadata(ISBN = null, ASIN = "B002RI9Z9E")),
    )
    val withIsbn = libraryItem(
      id = item.id,
      media = media(metadata = mediaMetadata(ISBN = "9780765393043", ASIN = "B002RI9Z9E")),
    )

    registry.observeCommunityInfo(asinOnly).first { it is LoadState.Loaded<*> }
    // The store serves the stale row first, then refetches with the new
    // identifiers — wait for the refetch to land.
    registry.observeCommunityInfo(withIsbn).first {
      it is LoadState.Loaded<*> && provider.bookInfoRequests.size == 2
    }

    assertThat(provider.bookInfoRequests.size).isEqualTo(2)
  }

  @Test
  fun `a provider that cannot serve the match is skipped for one that can`() = runTest {
    // Mimics an ASIN-only audiobook: the first-priority provider is ISBN-keyed
    // and declares itself unservable, so the lower-priority one serves.
    val isbnOnly = FakeBookInfoProvider(id = ProviderId.OpenLibrary, displayName = "ISBN Only")
    isbnOnly.canServeResult = false
    val asinCapable = FakeBookInfoProvider(id = ProviderId.Audnexus, displayName = "ASIN Capable")
    asinCapable.bookInfoResult = BookInfoResult.Success(communityInfo)

    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    BookInfoDatabase.Schema.synchronous().create(driver)
    val db = BookInfoDatabase(driver)
    val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
    val providers = setOf(isbnOnly, asinCapable)
    val registry = DefaultBookInfoRegistry(
      providers = providers,
      settings = DefaultBookInfoProviderSettings(MapSettings(), session),
      store = BookInfoStore(providers, db, dispatchers),
      userSession = session,
    )

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    val loaded = (state as LoadState.Loaded).data
    assertThat(loaded!!.providerId).isEqualTo(ProviderId.Audnexus)
    assertThat(loaded.availableSources.map { it.id }).isEqualTo(listOf(ProviderId.Audnexus))
    assertThat(isbnOnly.bookInfoRequests.size).isEqualTo(0)
  }

  @Test
  fun `clearing the cache forces a refetch`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }
    registry.clearCache()
    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    assertThat(provider.bookInfoRequests.size).isEqualTo(2)
  }

  @Test
  fun `a provider miss is cached as no info`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.NotFound
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }
    registry.observeCommunityInfo(item).first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data).isNull()
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }
}
