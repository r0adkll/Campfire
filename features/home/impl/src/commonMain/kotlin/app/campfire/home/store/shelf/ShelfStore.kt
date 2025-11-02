package app.campfire.home.store.shelf

import app.campfire.CampfireDatabase
import app.campfire.account.api.TokenHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.Cork
import app.campfire.core.model.ShelfEntity
import app.campfire.core.model.ShelfType
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.home.api.model.ShelfId
import kotlin.time.Duration.Companion.minutes
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object ShelfStore : Cork {

  override val tag = "ShelfStore"

  @Inject
  class Factory(
    db: CampfireDatabase,
    libraryItemDao: LibraryItemDao,
    tokenHydrator: TokenHydrator,
    dispatcherProvider: DispatcherProvider,
  ) {

    private val sourceOfTruthFactory = ShelfSourceOfTruthFactory(
      db = db,
      libraryItemDao = libraryItemDao,
      tokenHydrator = tokenHydrator,
      dispatcherProvider = dispatcherProvider,
    )

    fun create(): Store<Key, List<ShelfEntity>> {
      return StoreBuilder
        .from(
          fetcher = Fetcher.of { },
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
