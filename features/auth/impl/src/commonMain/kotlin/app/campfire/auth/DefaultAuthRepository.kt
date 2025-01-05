package app.campfire.auth

import app.campfire.CampfireDatabase
import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.auth.api.AuthRepository
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.AppScope
import app.campfire.core.model.Tent
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val accountManager: AccountManager,
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
) : AuthRepository {

  override suspend fun ping(serverUrl: String): Boolean {
    return api.ping(serverUrl)
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
  ): Result<Unit> {
    val result = api.login(serverUrl, username, password)

    if (result.isSuccess) {
      val response = result.getOrThrow()

      // Insert Server & User
      db.transaction {
        db.serversQueries.insert(
          response.serverSettings.asDatabaseModel(
            url = serverUrl,
            name = serverName,
            tent = tent,
          ),
        )

        // Insert User
        db.usersQueries.insert(
          response.user.asDatabaseModel(serverUrl, response.userDefaultLibraryId),
        )

        // Insert User MediaProgress
        response.user.mediaProgress.forEach { progress ->
          db.mediaProgressQueries.insert(progress.asDbModel())
        }

        // Insert User Bookmarks
        response.user.bookmarks.forEach { bookmark ->
          db.bookmarksQueries.insert(bookmark.asDbModel(response.user.id))
        }
      }

      // Store the access token into secure storage
      // For later use in authentication.
      accountManager.setToken(serverUrl, response.user.token)

      // Update the stored current user id. This value is used to reconstruct the session on new app launches
      settings.currentUserId = response.user.id

      // Update the current session manager which should trigger updates in the monitoring root parts of our
      // UI. Thus transitioning a user from logged out -> logged in, or from account to account.
      userSessionManager.current = UserSession.LoggedIn(
        user = response.user.asDomainModel(serverUrl, response.userDefaultLibraryId),
      )

      return Result.success(Unit)
    } else {
      return result.map { Unit }
    }
  }
}
