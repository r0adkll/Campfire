package app.campfire.home.progress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.mapping.asDomainModel
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext

@ContributesBinding(UserScope::class)
@Inject
class DbMediaProgressDatasource(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressDataSource {

  override suspend fun getMediaProgress(libraryItemId: LibraryItemId): MediaProgress? {
    return withContext(dispatcherProvider.databaseRead) {
      db.mediaProgressQueries.selectForLibraryItem(userSession.userId!!, libraryItemId)
        .executeAsOneOrNull()
        ?.asDomainModel()
    }
  }
}
