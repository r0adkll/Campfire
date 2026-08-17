// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth

import app.campfire.account.api.AccountManager
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.auth.di.ExistingUser
import app.campfire.auth.di.NewUser
import app.campfire.auth.local.UserStorageStrategy
import app.campfire.auth.model.asDomainModel
import app.campfire.core.di.AppScope
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.ApiException
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.envelopes.LoginResponse
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AuthAudioBookShelfApi,
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
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    val result = api.login(serverUrl, username, password, networkSettings?.extraHeaders)
    return processLoginResult(
      result = result,
      serverUrl = serverUrl,
      serverName = serverName,
      userId = userId,
      networkSettings = networkSettings,
    )
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    val result = api.oauth(serverUrl, state, code, codeVerifier, networkSettings?.extraHeaders)
    return processLoginResult(
      result = result,
      serverUrl = serverUrl,
      serverName = serverName,
      userId = userId,
      networkSettings = networkSettings,
    )
  }

  override suspend fun getNetworkSettings(userId: UserId): NetworkSettings? {
    return accountManager.getExtraHeaders(userId)?.let { extraHeaders ->
      NetworkSettings(extraHeaders = extraHeaders)
    }
  }

  private suspend fun processLoginResult(
    result: Result<LoginResponse>,
    serverUrl: String,
    serverName: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    val response = result.getOrElse { return Result.failure(it.asAuthException()) }

    if (response.user.accessToken == null) {
      return Result.failure(AuthException.UnexpectedResponse("No valid tokens found in the login response"))
    }

    val defaultLibraryId = response.userDefaultLibraryId
      ?: return Result.failure(AuthException.NoAccessibleLibraries())

    handleLoginResponse(
      serverUrl = serverUrl,
      serverName = serverName,
      response = response,
      defaultLibraryId = defaultLibraryId,
      userId = userId,
      networkSettings = networkSettings,
    )

    return Result.success(Unit)
  }

  private suspend fun handleLoginResponse(
    serverUrl: String,
    serverName: String,
    response: LoginResponse,
    defaultLibraryId: String,
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
      serverName = serverName,
      serverUrl = serverUrl,
      serverSettings = response.serverSettings,
      user = response.user,
      userDefaultLibraryId = defaultLibraryId,
    )

    // Add the new account/user and set it as the current session
    accountManager.addAccount(
      serverUrl = serverUrl,
      accessToken = requireNotNull(response.user.accessToken),
      refreshToken = response.user.refreshToken,
      extraHeaders = networkSettings?.extraHeaders,
      user = response.user.asDomainModel(serverUrl, defaultLibraryId),
    )
  }

  private fun Throwable.asAuthException(): AuthException = when {
    this is AuthException -> this
    this is IOException -> AuthException.Network(this)
    this is ApiException && statusCode == HTTP_UNAUTHORIZED -> AuthException.InvalidCredentials(this)
    else -> AuthException.UnexpectedResponse("The server returned an unexpected response", this)
  }

  companion object {
    private const val HTTP_UNAUTHORIZED = 401
  }
}
