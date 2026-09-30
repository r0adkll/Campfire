// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.RestorableAccount
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.PasswordCredentials
import app.campfire.auth.api.model.AUTH_METHOD_LOCAL
import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.UserId
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.network.oidc.OpenIdAuthorization
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update

internal class FakeRestorableAccountRepository(
  accounts: List<RestorableAccount>,
) : RestorableAccountRepository {
  private val accounts = MutableStateFlow(accounts)
  override fun observeRestorableAccounts(): Flow<List<RestorableAccount>> = accounts
  override suspend fun dismiss(account: RestorableAccount) {
    accounts.update { it - account }
  }
}

internal class FakeAuthRepository(
  private val authMethods: List<String> = listOf(AUTH_METHOD_LOCAL),
) : AuthRepository {
  val authenticatedUserNames = mutableListOf<String>()
  val authenticatedPasswords = mutableListOf<String>()

  override suspend fun status(serverUrl: String, networkSettings: NetworkSettings?): Result<ServerStatus> =
    Result.success(
      ServerStatus(
        serverVersion = "2.30.0",
        isInit = true,
        language = "en-us",
        authMethods = authMethods,
      ),
    )

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> {
    authenticatedUserNames += username
    authenticatedPasswords += password
    return Result.success(Unit)
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    codeVerifier: String,
    code: String,
    state: String,
    userId: UserId?,
    networkSettings: NetworkSettings?,
  ): Result<Unit> = Result.failure(IllegalStateException("Unreachable"))

  override suspend fun restore(account: BackedUpAccount, activate: Boolean): Result<Unit> =
    Result.failure(IllegalStateException("Unused"))

  override suspend fun getNetworkSettings(userId: UserId): NetworkSettings? = null
}

/**
 * A password manager holding [saved] passwords by `serverUrl to userName`
 */
internal class FakePasswordCredentials(
  private val saved: Map<Pair<String, String>, String> = emptyMap(),
) : PasswordCredentials {
  val offeredToSave = mutableListOf<Triple<String, String, String>>()
  val lookups = mutableListOf<Pair<String, String>>()

  override fun offerToSave(serverUrl: String, userName: String, password: String) {
    offeredToSave += Triple(serverUrl, userName, password)
  }

  override suspend fun find(serverUrl: String, userName: String): String? {
    lookups += serverUrl to userName
    return saved[serverUrl to userName]
  }
}

internal class UnusedAuthorizationFlow : AuthorizationFlow {
  override suspend fun getAuthorization(
    serverUrl: String,
    extraHeaders: Map<String, String>?,
  ): Result<OpenIdAuthorization> = error("Unused")
}

internal class GrantedLocalNetworkPermission : LocalNetworkPermissionController {
  override suspend fun requestIfNeeded(serverUrl: String): Boolean = true
}

internal class RecordingAppThemeRepository : AppThemeRepository {
  val appliedThemes = mutableListOf<AppTheme>()

  override fun observeCurrentAppTheme(): StateFlow<AppTheme> = MutableStateFlow(AppTheme.Fixed.Forest)
  override fun observeCustomThemes(): Flow<List<AppTheme.Fixed>> = emptyFlow()
  override fun setCurrentTheme(theme: AppTheme) {
    appliedThemes += theme
  }
  override suspend fun getCustomTheme(id: String): Result<AppTheme.Fixed> = error("Unused")
  override suspend fun saveCustomTheme(theme: AppTheme.Fixed) = Unit
  override suspend fun deleteCustomTheme(id: String) = Unit
}
