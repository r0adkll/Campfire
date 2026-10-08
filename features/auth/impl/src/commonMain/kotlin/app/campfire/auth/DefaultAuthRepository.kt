// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth

import app.campfire.account.api.AccountManager
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.ServerRepository
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.model.ServerStatus
import app.campfire.auth.di.ExistingUser
import app.campfire.auth.di.NewUser
import app.campfire.auth.local.UserStorageStrategy
import app.campfire.auth.model.asDomainModel
import app.campfire.core.di.AppScope
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.data.mapping.asDomainModel
import app.campfire.network.ApiException
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.network.envelopes.LoginResponse
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.io.IOException

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AuthAudioBookShelfApi,
  private val accountManager: AccountManager,
  private val serverRepository: ServerRepository,
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

  override suspend fun checkServerAvailable(serverUrl: String): Result<Unit> {
    val existing = serverRepository.getAllServers().firstOrNull { it.url.isSameServerAs(serverUrl) }
    return if (existing == null) {
      Result.success(Unit)
    } else {
      Result.failure(AuthException.ServerAlreadyAdded(existing.user.name))
    }
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    if (userId == null) checkServerAvailable(serverUrl).onFailure { return Result.failure(it) }
    val result = api.login(serverUrl, username, password, networkSettings?.extraHeaders)
    return processLoginResult(
      result = result,
      serverUrl = serverUrl,
      serverName = serverName,
      userId = userId,
      networkSettings = networkSettings,
    ).map { }
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
    if (userId == null) checkServerAvailable(serverUrl).onFailure { return Result.failure(it) }
    val result = api.oauth(serverUrl, state, code, codeVerifier, networkSettings?.extraHeaders)
    return processLoginResult(
      result = result,
      serverUrl = serverUrl,
      serverName = serverName,
      userId = userId,
      networkSettings = networkSettings,
    ).map { }
  }

  override suspend fun restore(account: BackedUpAccount, activate: Boolean): Result<User> {
    checkServerAvailable(account.serverUrl).onFailure { return Result.failure(it) }
    val refreshToken = account.token.refreshToken
      ?: return Result.failure(AuthException.InvalidCredentials())
    val networkSettings = account.extraHeaders
      .takeIf { it.isNotEmpty() }
      ?.let { NetworkSettings(extraHeaders = it) }

    val result = api.refresh(account.serverUrl, refreshToken, networkSettings?.extraHeaders)
    return processLoginResult(
      result = result,
      serverUrl = account.serverUrl,
      serverName = account.serverName,
      userId = null,
      networkSettings = networkSettings,
      activate = activate,
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
    activate: Boolean = true,
  ): Result<User> {
    val response = result.getOrElse { return Result.failure(it.asAuthException()) }

    if (response.user.accessToken == null) {
      return Result.failure(AuthException.UnexpectedResponse("No valid tokens found in the login response"))
    }

    val defaultLibraryId = response.userDefaultLibraryId
      ?: return Result.failure(AuthException.NoAccessibleLibraries())

    val user = handleLoginResponse(
      serverUrl = serverUrl,
      serverName = serverName,
      response = response,
      defaultLibraryId = defaultLibraryId,
      userId = userId,
      networkSettings = networkSettings,
      activate = activate,
    )

    return Result.success(user)
  }

  private suspend fun handleLoginResponse(
    serverUrl: String,
    serverName: String,
    response: LoginResponse,
    defaultLibraryId: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
    activate: Boolean,
  ): User {
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
    val user = response.user.asDomainModel(serverUrl, defaultLibraryId)
    accountManager.addAccount(
      serverUrl = serverUrl,
      accessToken = requireNotNull(response.user.accessToken),
      refreshToken = response.user.refreshToken,
      extraHeaders = networkSettings?.extraHeaders,
      user = user,
      activate = activate,
    )
    return user
  }

  // Server URLs are stored as typed or as the login probe resolved them
  private fun String.isSameServerAs(other: String): Boolean {
    return trim().trimEnd('/').equals(other.trim().trimEnd('/'), ignoreCase = true)
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
