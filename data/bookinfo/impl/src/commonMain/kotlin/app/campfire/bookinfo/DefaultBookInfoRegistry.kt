// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.CommunityContent
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.CommunitySource
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.api.ProviderStatus
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.api.bestMatch
import app.campfire.bookinfo.api.seriesMatch
import app.campfire.bookinfo.store.BookInfoStore
import app.campfire.bookinfo.store.CachedBookInfo
import app.campfire.bookinfo.store.SeriesInfoStore
import app.campfire.bookinfo.store.isStale
import app.campfire.bookinfo.store.mergeSeriesEntries
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItem
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadResponse

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultBookInfoRegistry(
  private val providers: Set<BookInfoProvider>,
  private val settings: BookInfoProviderSettings,
  private val store: BookInfoStore,
  private val seriesStore: SeriesInfoStore,
  private val userSession: UserSession,
) : BookInfoRegistry {

  override fun observeProviders(): Flow<List<ProviderStatus>> {
    if (providers.isEmpty()) return flowOf(emptyList())
    val statusFlows = providers
      .sortedBy { it.id.ordinal }
      .map { provider ->
        combine(settings.observeEnabled(provider.id), provider.observeLinkState()) { enabled, linkState ->
          ProviderStatus(provider, enabled, linkState)
        }
      }
    return combine(statusFlows) { it.toList() }
  }

  override fun observeCommunityInfo(
    item: LibraryItem,
    preferredProvider: ProviderId?,
  ): Flow<CommunityInfoState?> {
    val userId = userSession.userId ?: return flowOf(null)
    val match = item.bestMatch() ?: return flowOf(null)

    return combine(observeProviders(), settings.observePreferredProvider()) { statuses, stored ->
      val usable = statuses.filter { it.canServeBookInfo && it.provider.canServe(match) }
      // An enabled-but-unlinked review-capable provider is worth advertising
      // when whoever ends up serving has no review text of its own.
      val reviewsVia = statuses.firstOrNull {
        it.enabled &&
          it.provider.capabilities.hasReviewText &&
          it.linkState is ProviderLinkState.NotLinked
      }?.provider?.displayName
      // The caller's per-book choice wins over the persisted preference, which
      // wins over the default order.
      val status = usable.firstOrNull { it.provider.id == preferredProvider }
        ?: usable.firstOrNull { it.provider.id == stored }
        ?: usable.firstOrNull()
      Selection(status, usable, reviewsVia)
    }
      .distinctUntilChanged { old, new ->
        old.status?.provider?.id == new.status?.provider?.id &&
          old.status?.linkState == new.status?.linkState &&
          old.usable.map { it.provider.id to it.linkState } ==
          new.usable.map { it.provider.id to it.linkState } &&
          old.reviewsVia == new.reviewsVia
      }
      .flatMapLatest { (status, usable, reviewsVia) ->
        if (status == null) {
          flowOf(null)
        } else {
          streamFromStore(
            status = status,
            key = BookInfoStore.Key(userId, status.provider.id, item.id, match),
            sources = usable.map { CommunitySource(it.provider.id, it.provider.displayName) },
            reviewsLinkProviderName = reviewsVia
              .takeUnless { status.provider.capabilities.hasReviewText },
          )
        }
      }
  }

  private data class Selection(
    val status: ProviderStatus?,
    val usable: List<ProviderStatus>,
    val reviewsVia: String?,
  )

  override fun observeSeriesEntries(
    seriesName: String,
    ownedItems: List<LibraryItem>,
  ): Flow<LoadState<out SeriesInfoState>> {
    val ownedOnly = SeriesInfoState(
      providerId = null,
      providerName = null,
      isCompleted = null,
      entries = mergeSeriesEntries(ownedItems, series = null, providerId = ProviderId.Audible),
    )

    val userId = userSession.userId ?: return flowOf(LoadState.Loaded(ownedOnly))
    val match = seriesMatch(seriesName, ownedItems) ?: return flowOf(LoadState.Loaded(ownedOnly))

    return combine(observeProviders(), settings.observePreferredProvider()) { statuses, stored ->
      val usable = statuses.filter { it.canServeSeries }
      usable.firstOrNull { it.provider.id == stored } ?: usable.firstOrNull()
    }
      .distinctUntilChanged { old, new ->
        old?.provider?.id == new?.provider?.id && old?.linkState == new?.linkState
      }
      .flatMapLatest { status ->
        if (status == null) {
          flowOf(LoadState.Loaded(ownedOnly))
        } else {
          streamSeriesFromStore(
            status = status,
            key = SeriesInfoStore.Key(userId, status.provider.id, match),
            ownedItems = ownedItems,
            ownedOnly = ownedOnly,
          )
        }
      }
  }

  private fun streamSeriesFromStore(
    status: ProviderStatus,
    key: SeriesInfoStore.Key,
    ownedItems: List<LibraryItem>,
    ownedOnly: SeriesInfoState,
  ): Flow<LoadState<out SeriesInfoState>> = flow {
    // The user's own books are already local. Provider entries decorate that
    // list, so they must never gate it behind a network round-trip — show the
    // owned books immediately and merge provider entries in when they arrive.
    emit(LoadState.Loaded(ownedOnly))

    val cached = seriesStore.cached(key)
    val invalidLink = status.linkState is ProviderLinkState.Invalid
    if (invalidLink && cached == null) return@flow

    val refresh = !invalidLink &&
      (
        cached == null ||
          cached.isStale(
            nowMillis = Clock.System.now().toEpochMilliseconds(),
            currentMatchKey = key.match.cacheKey,
          )
        )

    seriesStore.stream(key, refresh = refresh).collect { response ->
      // Loading and Error keep the owned-only list on screen.
      if (response is StoreReadResponse.Data) {
        val series = response.value.series
        emit(
          LoadState.Loaded(
            SeriesInfoState(
              providerId = series?.let { status.provider.id },
              providerName = series?.let { status.provider.displayName },
              isCompleted = series?.isCompleted,
              entries = mergeSeriesEntries(ownedItems, series, status.provider.id),
            ),
          ),
        )
      }
    }
  }

  override suspend fun clearCache() {
    store.clearAll()
    seriesStore.clearAll()
  }

  private fun streamFromStore(
    status: ProviderStatus,
    key: BookInfoStore.Key,
    sources: List<CommunitySource>,
    reviewsLinkProviderName: String?,
  ): Flow<CommunityInfoState?> = flow {
    val base = CommunityInfoState(
      providerId = status.provider.id,
      providerName = status.provider.displayName,
      availableSources = sources,
      needsRelink = status.linkState is ProviderLinkState.Invalid,
      reviewsLinkProviderName = reviewsLinkProviderName,
    )
    val cached = store.cached(key)
    val invalidLink = status.linkState is ProviderLinkState.Invalid

    // A rejected token means fetches would just 401; serve whatever is cached.
    if (invalidLink && cached == null) {
      emit(base.copy(content = CommunityContent.Unavailable))
      return@flow
    }

    val refresh = !invalidLink &&
      (
        cached == null ||
          cached.isStale(
            nowMillis = Clock.System.now().toEpochMilliseconds(),
            currentMatchKey = key.match.cacheKey,
          )
        )

    var emittedData = false
    store.stream(key, refresh = refresh).collect { response ->
      when (response) {
        is StoreReadResponse.Loading -> if (!emittedData) emit(base)
        is StoreReadResponse.Data -> {
          emittedData = true
          emit(base.copy(content = response.value.toContent()))
        }
        is StoreReadResponse.Error -> if (!emittedData) {
          emit(base.copy(content = CommunityContent.Unavailable))
        }
        else -> Unit
      }
    }
  }

  private fun CachedBookInfo.toContent(): CommunityContent {
    return info
      ?.takeIf { it.rating != null }
      ?.let { CommunityContent.Available(it, reviews) }
      ?: CommunityContent.Unavailable
  }

  private val ProviderStatus.canServeBookInfo: Boolean
    get() = enabled &&
      provider.capabilities.hasAggregateRating &&
      linkState !is ProviderLinkState.NotLinked

  private val ProviderStatus.canServeSeries: Boolean
    get() = enabled &&
      provider.capabilities.hasSeriesOrdering &&
      linkState !is ProviderLinkState.NotLinked
}
