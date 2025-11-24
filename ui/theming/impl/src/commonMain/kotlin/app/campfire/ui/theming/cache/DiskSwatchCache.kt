package app.campfire.ui.theming.cache

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import app.campfire.themes.Swatch as DbSwatch
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.r0adkll.swatchbuckler.compose.Swatch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class DiskSwatchCache(
  private val db: CampfireThemeDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : DiskCache<Swatch> {

  override suspend fun selectAll(): Map<String, Swatch> {
    return withContext(dispatcherProvider.databaseRead) {
      db.swatchQueries.selectAll()
        .awaitAsList()
        .associate { it.key to Swatch(it.dominant, it.vibrant.orEmpty()) }
    }
  }

  override suspend fun get(key: String): Swatch? = withContext(dispatcherProvider.databaseRead) {
    db.swatchQueries.selectByKey(key) { _, dominant, vibrant ->
      Swatch(dominant, vibrant.orEmpty())
    }.awaitAsOneOrNull()
  }

  override suspend fun set(key: String, value: Swatch) {
    withContext(dispatcherProvider.databaseWrite) {
      db.swatchQueries.insert(DbSwatch(key, value.dominant, value.vibrant.takeIf { it.isNotEmpty() }))
    }
  }

  override suspend fun putAll(from: Map<String, Swatch>) {
    withContext(dispatcherProvider.databaseWrite) {
      db.swatchQueries.transaction {
        from.forEach { (key, value) ->
          db.swatchQueries.insert(DbSwatch(key, value.dominant, value.vibrant.takeIf { it.isNotEmpty() }))
        }
      }
    }
  }

  override suspend fun remove(key: String) {
    withContext(dispatcherProvider.databaseWrite) {
      db.swatchQueries.deleteByKey(key)
    }
  }

  override suspend fun containsKey(key: String): Boolean {
    return withContext(dispatcherProvider.databaseRead) {
      db.swatchQueries.containsKey(key).awaitAsOneOrNull() != null
    }
  }
}
