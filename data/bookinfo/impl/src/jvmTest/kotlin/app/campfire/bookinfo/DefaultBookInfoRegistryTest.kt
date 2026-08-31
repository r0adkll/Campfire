// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookCommunityInfo
import app.campfire.bookinfo.api.BookInfoResult
import app.campfire.bookinfo.api.CommunityContent
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.CommunitySource
import app.campfire.bookinfo.api.ProviderCapabilities
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.api.ProviderSeries
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.db.BookInfoDatabase
import app.campfire.bookinfo.store.BookInfoStore
import app.campfire.bookinfo.store.SeriesInfoStore
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
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
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
    vararg providers: FakeBookInfoProvider,
    settings: DefaultBookInfoProviderSettings = DefaultBookInfoProviderSettings(MapSettings(), session),
  ): DefaultBookInfoRegistry {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    BookInfoDatabase.Schema.synchronous().create(driver)
    val db = BookInfoDatabase(driver)
    val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
    val providerSet = providers.toSet<app.campfire.bookinfo.api.BookInfoProvider>()
    return DefaultBookInfoRegistry(
      providers = providerSet,
      settings = settings,
      store = BookInfoStore(providerSet, db, dispatchers),
      seriesStore = SeriesInfoStore(providerSet, db, dispatchers),
      userSession = session,
    )
  }

  private suspend fun Flow<CommunityInfoState?>.awaitAvailable(): CommunityInfoState {
    return first { it?.content is CommunityContent.Available }!!
  }

  @Test
  fun `providers are reported with enablement and link state`() = runTest {
    val registry = registry(FakeBookInfoProvider())

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

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Hardcover)
    assertThat(state.providerName).isEqualTo("Fake Provider")
    assertThat((state.content as CommunityContent.Available).info.rating).isEqualTo(4.63)
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `an uncached fetch reports its loading phase with provider context`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    // The first emission already names the serving provider and sources, so a
    // section rendered from it never disappears while the fetch runs.
    val first = registry.observeCommunityInfo(item).first()!!

    assertThat(first.content).isInstanceOf(CommunityContent.Loading::class)
    assertThat(first.providerName).isEqualTo("Fake Provider")
    assertThat(first.availableSources.map { it.id }).isEqualTo(listOf(ProviderId.Hardcover))
  }

  @Test
  fun `a fresh cache is served without refetching`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).awaitAvailable()
    registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `an unlinked provider yields no community info`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.linkState.value = ProviderLinkState.NotLinked
    val registry = registry(provider)

    assertThat(registry.observeCommunityInfo(item).first()).isNull()
  }

  @Test
  fun `a disabled provider yields no community info`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setEnabled(ProviderId.Hardcover, false)
    val registry = registry(provider, settings = settings)

    assertThat(registry.observeCommunityInfo(item).first()).isNull()
    assertThat(provider.bookInfoRequests.size).isEqualTo(0)
  }

  @Test
  fun `an invalid link serves cached data with a relink flag and no refetch`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).awaitAvailable()
    provider.linkState.value = ProviderLinkState.Invalid

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.needsRelink).isTrue()
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }

  @Test
  fun `community info carries the available sources`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.availableSources)
      .isEqualTo(listOf(CommunitySource(ProviderId.Hardcover, "Fake Provider")))
  }

  @Test
  fun `a preferred provider is used when it can serve`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry
      .observeCommunityInfo(item, preferredProvider = ProviderId.Hardcover)
      .awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Hardcover)
  }

  @Test
  fun `an unusable preferred provider falls back to the automatic pick`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry
      .observeCommunityInfo(item, preferredProvider = ProviderId.OpenLibrary)
      .awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Hardcover)
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

    registry.observeCommunityInfo(asinOnly).awaitAvailable()
    // The store serves the stale row first, then refetches with the new
    // identifiers — wait for the refetch to land.
    registry.observeCommunityInfo(withIsbn).first {
      it?.content is CommunityContent.Available && provider.bookInfoRequests.size == 2
    }

    assertThat(provider.bookInfoRequests.size).isEqualTo(2)
  }

  @Test
  fun `the persisted preferred provider wins over the default order`() = runTest {
    val hardcover = FakeBookInfoProvider(id = ProviderId.Hardcover, displayName = "Hardcover")
    hardcover.bookInfoResult = BookInfoResult.Success(communityInfo)
    val audible = FakeBookInfoProvider(id = ProviderId.Audible, displayName = "Audible")
    audible.bookInfoResult = BookInfoResult.Success(communityInfo)
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setPreferredProvider(ProviderId.Audible)
    val registry = registry(hardcover, audible, settings = settings)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Audible)
    // Both stay switchable in the pill.
    assertThat(state.availableSources.map { it.id })
      .isEqualTo(listOf(ProviderId.Hardcover, ProviderId.Audible))
  }

  @Test
  fun `a per book choice overrides the persisted preference`() = runTest {
    val hardcover = FakeBookInfoProvider(id = ProviderId.Hardcover, displayName = "Hardcover")
    hardcover.bookInfoResult = BookInfoResult.Success(communityInfo)
    val audible = FakeBookInfoProvider(id = ProviderId.Audible, displayName = "Audible")
    audible.bookInfoResult = BookInfoResult.Success(communityInfo)
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setPreferredProvider(ProviderId.Audible)
    val registry = registry(hardcover, audible, settings = settings)

    val state = registry
      .observeCommunityInfo(item, preferredProvider = ProviderId.Hardcover)
      .awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Hardcover)
  }

  @Test
  fun `an unusable persisted preference falls back to the default order`() = runTest {
    val hardcover = FakeBookInfoProvider(id = ProviderId.Hardcover, displayName = "Hardcover")
    hardcover.bookInfoResult = BookInfoResult.Success(communityInfo)
    val audible = FakeBookInfoProvider(id = ProviderId.Audible, displayName = "Audible")
    audible.canServeResult = false
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setPreferredProvider(ProviderId.Audible)
    val registry = registry(hardcover, audible, settings = settings)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Hardcover)
  }

  @Test
  fun `preferred provider setting round trips and clears`() = runTest {
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)

    assertThat(settings.preferredProvider()).isNull()
    settings.setPreferredProvider(ProviderId.OpenLibrary)
    assertThat(settings.preferredProvider()).isEqualTo(ProviderId.OpenLibrary)
    assertThat(settings.observePreferredProvider().first()).isEqualTo(ProviderId.OpenLibrary)
    settings.setPreferredProvider(null)
    assertThat(settings.preferredProvider()).isNull()
  }

  @Test
  fun `series selection respects the persisted preference`() = runTest {
    val hardcover = FakeBookInfoProvider(id = ProviderId.Hardcover, displayName = "Hardcover")
    val audible = FakeBookInfoProvider(id = ProviderId.Audible, displayName = "Audible")
    audible.seriesResult = BookInfoResult.NotFound
    val settings = DefaultBookInfoProviderSettings(MapSettings(), session)
    settings.setPreferredProvider(ProviderId.Audible)
    val owned = libraryItem(
      media = media(metadata = mediaMetadata(title = "The Way of Kings", ASIN = "B003P2WO5E")),
    )
    val registry = registry(hardcover, audible, settings = settings)

    registry.observeSeriesEntries("The Stormlight Archive", listOf(owned))
      .first { audible.seriesRequests.isNotEmpty() || hardcover.seriesRequests.isNotEmpty() }

    assertThat(audible.seriesRequests.size).isEqualTo(1)
    assertThat(hardcover.seriesRequests.size).isEqualTo(0)
  }

  @Test
  fun `a provider that cannot serve the match is skipped for one that can`() = runTest {
    // Mimics an ASIN-only audiobook: the first-priority provider is ISBN-keyed
    // and declares itself unservable, so the lower-priority one serves.
    val isbnOnly = FakeBookInfoProvider(id = ProviderId.OpenLibrary, displayName = "ISBN Only")
    isbnOnly.canServeResult = false
    val asinCapable = FakeBookInfoProvider(id = ProviderId.Audible, displayName = "ASIN Capable")
    asinCapable.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(isbnOnly, asinCapable)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.Audible)
    assertThat(state.availableSources.map { it.id }).isEqualTo(listOf(ProviderId.Audible))
    assertThat(isbnOnly.bookInfoRequests.size).isEqualTo(0)
  }

  @Test
  fun `a linkable review source is advertised when the serving provider lacks reviews`() = runTest {
    val keyless = FakeBookInfoProvider(
      id = ProviderId.OpenLibrary,
      displayName = "Open Library",
      capabilities = ProviderCapabilities(hasAggregateRating = true),
    )
    keyless.bookInfoResult = BookInfoResult.Success(communityInfo)
    val hardcover = FakeBookInfoProvider(id = ProviderId.Hardcover, displayName = "Hardcover")
    hardcover.linkState.value = ProviderLinkState.NotLinked
    val registry = registry(hardcover, keyless)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.providerId).isEqualTo(ProviderId.OpenLibrary)
    assertThat(state.reviewsLinkProviderName).isEqualTo("Hardcover")
  }

  @Test
  fun `no review link is advertised when the serving provider has review text`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(state.reviewsLinkProviderName).isNull()
  }

  @Test
  fun `owned books are emitted before the series provider responds`() = runTest {
    val provider = FakeBookInfoProvider()
    val owned = libraryItem(
      media = media(metadata = mediaMetadata(title = "The Way of Kings", ASIN = "B003P2WO5E")),
    )
    val registry = registry(provider)

    // The very first loaded emission must already carry the user's own book —
    // provider entries decorate the list, they never gate it.
    val first = registry
      .observeSeriesEntries("The Stormlight Archive", listOf(owned))
      .first { it is LoadState.Loaded<*> }

    val loaded = (first as LoadState.Loaded).data
    assertThat(loaded.entries.map { it::class.simpleName }).isEqualTo(listOf("Owned"))
    assertThat(loaded.providerId).isNull()
  }

  @Test
  fun `series entries merge owned books with provider entries`() = runTest {
    val provider = FakeBookInfoProvider()
    val owned = libraryItem(
      media = media(metadata = mediaMetadata(title = "The Way of Kings", ASIN = "B003P2WO5E")),
    )
    provider.seriesResult = BookInfoResult.Success(
      ProviderSeries(
        providerSeriesId = "B005NB27MK",
        name = "The Stormlight Archive",
        isCompleted = null,
        entries = listOf(
          ProviderSeriesEntry(
            providerBookId = "B003P2WO5E",
            position = 1.0,
            title = "The Way of Kings",
            releaseDate = "2010-08-31",
            isReleased = true,
            providerUrl = null,
            coverUrl = null,
            asins = listOf("B003P2WO5E"),
          ),
          ProviderSeriesEntry(
            providerBookId = "B00BWWSVPU",
            position = 2.0,
            title = "Words of Radiance",
            releaseDate = "2014-03-04",
            isReleased = true,
            providerUrl = null,
            coverUrl = null,
          ),
          ProviderSeriesEntry(
            providerBookId = "B0UPCOMING",
            position = 6.0,
            title = "Untitled #6",
            releaseDate = "2031-01-01",
            isReleased = false,
            providerUrl = null,
            coverUrl = null,
          ),
        ),
      ),
    )
    val registry = registry(provider)

    val state = registry
      .observeSeriesEntries("The Stormlight Archive", listOf(owned))
      .first { it is LoadState.Loaded<*> && (it as LoadState.Loaded).data.providerId != null }

    val loaded = (state as LoadState.Loaded).data
    assertThat(loaded.providerName).isEqualTo("Fake Provider")
    assertThat(loaded.entries.map { it::class.simpleName })
      .isEqualTo(listOf("Owned", "Missing", "Upcoming"))
  }

  @Test
  fun `series without identifiable members never calls the provider`() = runTest {
    val provider = FakeBookInfoProvider()
    val owned = libraryItem(
      media = media(metadata = mediaMetadata(title = "Untracked", ISBN = null, ASIN = null)),
    )
    val registry = registry(provider)

    val state = registry
      .observeSeriesEntries("Mystery Series", listOf(owned))
      .first { it is LoadState.Loaded<*> }

    assertThat((state as LoadState.Loaded).data.entries.size).isEqualTo(1)
    assertThat(provider.seriesRequests.size).isEqualTo(0)
  }

  @Test
  fun `clearing the cache forces a refetch`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.Success(communityInfo)
    val registry = registry(provider)

    registry.observeCommunityInfo(item).awaitAvailable()
    registry.clearCache()
    registry.observeCommunityInfo(item).awaitAvailable()

    assertThat(provider.bookInfoRequests.size).isEqualTo(2)
  }

  @Test
  fun `a provider miss keeps the section with empty content`() = runTest {
    val provider = FakeBookInfoProvider()
    provider.bookInfoResult = BookInfoResult.NotFound
    val registry = registry(provider)

    val state = registry.observeCommunityInfo(item)
      .first { it?.content is CommunityContent.Unavailable }!!
    registry.observeCommunityInfo(item).first { it?.content is CommunityContent.Unavailable }

    // The miss keeps the provider context (pill, sources) and is cached.
    assertThat(state.providerName).isEqualTo("Fake Provider")
    assertThat(provider.bookInfoRequests.size).isEqualTo(1)
  }
}
