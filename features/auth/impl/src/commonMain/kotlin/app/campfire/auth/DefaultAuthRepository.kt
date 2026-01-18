package app.campfire.auth

import app.campfire.CampfireDatabase
import app.campfire.account.api.AccountManager
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.auth.di.ExistingUser
import app.campfire.auth.di.NewUser
import app.campfire.auth.local.UserStorageStrategy
import app.campfire.auth.model.asDomainModel
import app.campfire.core.di.AppScope
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.Tent
import app.campfire.core.model.UserId
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
  @NewUser private val newUserStorageStrategy: UserStorageStrategy,
  @ExistingUser private val existingUserStorageStrategy: UserStorageStrategy,
) : AuthRepository {

  override suspend fun status(
    serverUrl: String,
    networkSettings: NetworkSettings?,
  ): Result<ServerStatus> {
    return api.status(serverUrl, networkSettings?.extraHeaders)
      .map { it.asDomainModel() }
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    val result = api.login(serverUrl, username, password, networkSettings?.extraHeaders)

    if (result.isSuccess) {
      val response = result.getOrThrow()

      handleLoginResponse(
        serverUrl = serverUrl,
        serverName = serverName,
        tent = tent,
        response = response,
        userId = userId,
        networkSettings = networkSettings,
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
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    val result = api.oauth(serverUrl, state, code, codeVerifier, networkSettings?.extraHeaders)

    if (result.isSuccess) {
      val response = result.getOrThrow()
      if (response.user.accessToken == null) {
        return Result.failure(IllegalStateException("Unable to authenticate user. No valid tokens."))
      }

      handleLoginResponse(
        serverUrl = serverUrl,
        serverName = serverName,
        tent = tent,
        response = response,
        userId = userId,
        networkSettings = networkSettings,
      )

      return Result.success(Unit)
    } else {
      return result.map { Unit }
    }
  }

  override suspend fun getNetworkSettings(userId: UserId): NetworkSettings? {
    return accountManager.getExtraHeaders(userId)?.let { extraHeaders ->
      NetworkSettings(extraHeaders = extraHeaders)
    }
  }

  private suspend fun handleLoginResponse(
    serverUrl: String,
    serverName: String,
    tent: Tent,
    response: LoginResponse,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ) {
    // Insert Server & User
    val storageStrategy = if (userId != null) {
      existingUserStorageStrategy
    } else {
      newUserStorageStrategy
    }

    storageStrategy.store(
      tent = tent,
      serverName = serverName,
      serverUrl = serverUrl,
      serverSettings = response.serverSettings,
      user = response.user,
      userDefaultLibraryId = response.userDefaultLibraryId,
    )

    // Add the new account/user and set it as the current session
    accountManager.addAccount(
      serverUrl = serverUrl,
      accessToken = requireNotNull(response.user.accessToken),
      refreshToken = response.user.refreshToken,
      extraHeaders = networkSettings?.extraHeaders,
      user = response.user.asDomainModel(serverUrl, response.userDefaultLibraryId),
    )
  }
}
