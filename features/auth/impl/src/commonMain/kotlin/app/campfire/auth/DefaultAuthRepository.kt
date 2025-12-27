package app.campfire.auth

import app.campfire.CampfireDatabase
import app.campfire.account.api.AccountManager
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.auth.model.asDomainModel
import app.campfire.core.di.AppScope
import app.campfire.core.model.Tent
import app.campfire.data.mapping.asDatabaseModel
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.envelopes.LoginResponse
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AuthAudioBookShelfApi,
  private val db: CampfireDatabase,
  private val accountManager: AccountManager,
) : AuthRepository {

  override suspend fun status(serverUrl: String): Result<ServerStatus> {
    return api.status(serverUrl)
      .map { it.asDomainModel() }
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

      handleLoginResponse(
        serverUrl = serverUrl,
        serverName = serverName,
        tent = tent,
        response = response,
      )

      return Result.success(Unit)
    } else {
      return result.map { Unit }
    }
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    tent: Tent,
  ): Result<Unit> {
    val result = api.oauth(serverUrl, state, code, codeVerifier)

    if (result.isSuccess) {
      val response = result.getOrThrow()

      handleLoginResponse(
        serverUrl = serverUrl,
        serverName = serverName,
        tent = tent,
        response = response,
      )

      return Result.success(Unit)
    } else {
      return result.map { Unit }
    }
  }

  private suspend fun handleLoginResponse(
    serverUrl: String,
    serverName: String,
    tent: Tent,
    response: LoginResponse,
  ) {
    // Insert Server & User
    db.transaction {
      db.serversQueries.insert(
        response.serverSettings.asDatabaseModel(
          url = serverUrl,
          userId = response.user.id,
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

    // Add the new account/user and set it as the current session
    accountManager.addAccount(
      serverUrl = serverUrl,
      accessToken = response.user.accessToken,
      refreshToken = response.user.refreshToken,
      user = response.user.asDomainModel(serverUrl, response.userDefaultLibraryId),
    )
  }
}
