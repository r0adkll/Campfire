// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.store.shelf

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.home.api.model.ShelfId
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object ShelfStore : Cork {

  override val tag = "ShelfStore"
  override val enabled: Boolean = false

  @Inject
  class Factory(
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    urlHydrator: UrlHydrator,
    dispatcherProvider: DispatcherProvider,
    userSession: UserSession,
  ) {

    private val sourceOfTruthFactory = ShelfSourceOfTruthFactory(
      db = db,
      libraryItemDao = libraryItemDao,
      urlHydrator = urlHydrator,
      dispatcherProvider = dispatcherProvider,
      userSession = userSession,
    )

    fun create(): Store<Key, List<ShelfEntity>> {
      return StoreBuilder
        .from(
          fetcher = Fetcher.ofResult { FetcherResult.Error.Message("No network for this store") },
          sourceOfTruth = sourceOfTruthFactory.create(),
        )
        .cachePolicy(
          MemoryPolicy.builder<Key, List<ShelfEntity>>()
            .setExpireAfterAccess(5.minutes)
            .build(),
        )
        .build()
    }
  }

  data class Key(
    val shelfId: ShelfId,
    val type: ShelfType,
  )
}
