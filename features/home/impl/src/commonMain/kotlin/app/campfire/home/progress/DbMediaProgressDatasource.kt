package app.campfire.home.progress

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.data.mapping.asDomainModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@ContributesBinding(UserScope::class)
@Inject
class DbMediaProgressDatasource(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : MediaProgressDataSource {

  override fun observeMediaProgress(ids: Set<LibraryItemId>): Flow<Map<LibraryItemId, MediaProgress>> {
    return db.mediaProgressQueries
      .selectForLibraryItems(userSession.requiredUserId, ids)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .map { progresses ->
        progresses
          .map { it.asDomainModel() }
          .associateBy { it.libraryItemId }
      }
  }
}
