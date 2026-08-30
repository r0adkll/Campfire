// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo

import app.campfire.bookinfo.api.BookInfoProvider
import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.CommunityInfoState
import app.campfire.bookinfo.api.CommunitySource
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderLinkState
import app.campfire.bookinfo.api.ProviderStatus
import app.campfire.bookinfo.api.bestMatch
import app.campfire.bookinfo.store.BookInfoStore
import app.campfire.bookinfo.store.CachedBookInfo
import app.campfire.bookinfo.store.isStale
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
  ): Flow<LoadState<out CommunityInfoState?>> {
    val userId = userSession.userId ?: return flowOf(LoadState.Loaded(null))
    val match = item.bestMatch() ?: return flowOf(LoadState.Loaded(null))

    return observeProviders()
      .map { statuses -> statuses.filter { it.canServeBookInfo } }
      .distinctUntilChanged { old, new ->
        old.map { it.provider.id to it.linkState } == new.map { it.provider.id to it.linkState }
      }
      .flatMapLatest { usable ->
        val status = usable.firstOrNull { it.provider.id == preferredProvider }
          ?: usable.firstOrNull()
        if (status == null) {
          flowOf(LoadState.Loaded(null))
        } else {
          streamFromStore(
            status = status,
            key = BookInfoStore.Key(userId, status.provider.id, item.id, match),
            sources = usable.map { CommunitySource(it.provider.id, it.provider.displayName) },
          )
        }
      }
  }

  override suspend fun clearCache() {
    store.clearAll()
  }

  private fun streamFromStore(
    status: ProviderStatus,
    key: BookInfoStore.Key,
    sources: List<CommunitySource>,
  ): Flow<LoadState<out CommunityInfoState?>> = flow {
    val cached = store.cached(key)
    val invalidLink = status.linkState is ProviderLinkState.Invalid

    // A rejected token means fetches would just 401; serve whatever is cached.
    if (invalidLink && cached == null) {
      emit(LoadState.Loaded(null))
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
        is StoreReadResponse.Loading -> if (!emittedData) emit(LoadState.Loading)
        is StoreReadResponse.Data -> {
          emittedData = true
          emit(LoadState.Loaded(response.value.toState(status, sources)))
        }
        is StoreReadResponse.Error -> if (!emittedData) emit(LoadState.Loaded(null))
        else -> Unit
      }
    }
  }

  private fun CachedBookInfo.toState(
    status: ProviderStatus,
    sources: List<CommunitySource>,
  ): CommunityInfoState? {
    val info = info ?: return null
    return CommunityInfoState(
      providerId = status.provider.id,
      providerName = status.provider.displayName,
      providerUrl = info.providerUrl,
      info = info,
      reviews = reviews,
      needsRelink = status.linkState is ProviderLinkState.Invalid,
      availableSources = sources,
    )
  }

  private val ProviderStatus.canServeBookInfo: Boolean
    get() = enabled &&
      provider.capabilities.hasAggregateRating &&
      linkState !is ProviderLinkState.NotLinked
}
