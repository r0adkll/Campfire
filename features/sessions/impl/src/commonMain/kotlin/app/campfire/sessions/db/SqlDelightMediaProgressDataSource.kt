package app.campfire.sessions.db

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class SqlDelightMediaProgressDataSource(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressDataSource {

  override suspend fun deleteMediaProgress(libraryItemId: LibraryItemId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.mediaProgressQueries.delete(libraryItemId)
    }
  }
}
