package app.campfire.ui.theming.cache

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import app.campfire.themes.Theme as DbTheme
import app.campfire.ui.theming.db.mapping.asDbModel
import app.campfire.ui.theming.db.mapping.asDomainTheme
import app.campfire.ui.theming.theme.ComputedTheme
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class DiskThemeCache(
  private val db: CampfireThemeDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : DiskCache<ComputedTheme> {

  override suspend fun selectAll(): Map<String, ComputedTheme> {
    return withContext(dispatcherProvider.databaseRead) {
      db.themeQueries.selectAllThemes()
        .awaitAsList()
        // We share this table with custom app themes so let's ignore them for this cache
        .filter { it.key.startsWith("custom") }
        .associate { it.cacheKey to it.asDomainTheme() }
    }
  }

  override suspend fun get(key: String): ComputedTheme? = withContext(dispatcherProvider.databaseRead) {
    db.themeQueries.selectThemeForKey(key)
      .awaitAsOneOrNull()
      ?.asDomainTheme()
  }

  override suspend fun set(key: String, value: ComputedTheme) {
    withContext(dispatcherProvider.databaseWrite) {
      db.themeQueries.transaction {
        insertComputedTheme(key, value)
      }
    }
  }

  override suspend fun putAll(from: Map<String, ComputedTheme>) {
    withContext(dispatcherProvider.databaseWrite) {
      db.themeQueries.transaction {
        from.forEach { (key, value) ->
          insertComputedTheme(key, value)
        }
      }
    }
  }

  @Suppress("UnusedReceiverParameter")
  private suspend fun SuspendingTransactionWithoutReturn.insertComputedTheme(key: String, value: ComputedTheme) {
    db.themeQueries.insertTheme(DbTheme(key, value.key))
    db.themeQueries.insertColorScheme(
      value.theme.lightColorScheme.asDbModel(key, false),
    )
    db.themeQueries.insertColorScheme(
      value.theme.darkColorScheme.asDbModel(key, true),
    )
  }

  override suspend fun remove(key: String) {
    // Its probable here that the cacheKey is going to be different (if we need to remove a previous computed theme
    // that used different parameters then the resulting cacheKey from the pipeline is just going to be different)
    withContext(dispatcherProvider.databaseWrite) {
      db.themeQueries.deleteTheme(key)
    }
  }

  override suspend fun containsKey(key: String): Boolean {
    return withContext(dispatcherProvider.databaseRead) {
      db.themeQueries.containsKey(key).awaitAsOneOrNull() != null
    }
  }
}
