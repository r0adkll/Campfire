package app.campfire.auth.local

import app.campfire.CampfireDatabase
import app.campfire.auth.di.NewUser
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.model.Tent
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.ServerSettings
import app.campfire.network.models.User
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@NewUser
@ContributesBinding(AppScope::class)
@Inject
class NewUserStorageStrategy(
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserStorageStrategy {

  override suspend fun store(
    tent: Tent,
    serverName: String,
    serverUrl: String,
    serverSettings: ServerSettings,
    user: User,
    userDefaultLibraryId: String,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.serversQueries.insert(
        serverSettings.asDatabaseModel(
          url = serverUrl,
          userId = user.id,
          name = serverName,
          tent = tent,
        ),
      )

      // Insert User
      db.usersQueries.insert(
        user.asDatabaseModel(serverUrl, userDefaultLibraryId),
      )

      // Insert User MediaProgress
      user.mediaProgress.forEach { progress ->
        db.mediaProgressQueries.insert(progress.asDbModel())
      }

      // Insert User Bookmarks
      user.bookmarks.forEach { bookmark ->
        db.bookmarksQueries.insert(bookmark.asDbModel(user.id))
      }
    }
  }
}
