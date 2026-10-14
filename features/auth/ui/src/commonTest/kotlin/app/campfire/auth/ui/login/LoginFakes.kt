// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.account.api.BackedUpAccount
import app.campfire.account.api.RestorableAccount
import app.campfire.account.api.RestorableAccountRepository
import app.campfire.auth.api.AuthException
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.api.ConnectLinkInbox
import app.campfire.auth.api.PasswordCredentials
import app.campfire.auth.api.model.AUTH_METHOD_LOCAL
import app.campfire.auth.api.model.ServerStatus
import app.campfire.core.model.NetworkSettings
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.navigation.ConnectLinkResult
import app.campfire.core.permission.LocalNetworkPermissionController
import app.campfire.network.oidc.AuthorizationFlow
import app.campfire.network.oidc.OpenIdAuthorization
import app.campfire.qrscanner.QrCodeScanner
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

/**
 * Like the real one, a sign-in without a `userId` fails when [addedServers] (server URL to the
 * signed-in user's name) already has an account on that server.
 */
internal class FakeAuthRepository(
  private val authMethods: List<String> = listOf(AUTH_METHOD_LOCAL),
  private val addedServers: Map<String, String> = emptyMap(),
  /** When set, the server sits behind Cloudflare Access and only lets this Client ID through. */
  private val accessClientId: String? = null,
) : AuthRepository {
  val authenticatedUserNames = mutableListOf<String>()
  val authenticatedPasswords = mutableListOf<String>()

  override suspend fun status(serverUrl: String, networkSettings: NetworkSettings?): Result<ServerStatus> {
    if (accessClientId != null && networkSettings?.extraHeaders?.get("CF-Access-Client-Id") != accessClientId) {
      return Result.failure(AuthException.CloudflareAccess())
    }
    return Result.success(
      ServerStatus(
        serverVersion = "2.30.0",
        isInit = true,
        language = "en-us",
        authMethods = authMethods,
      ),
    )
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

  override suspend fun checkServerAvailable(serverUrl: String): Result<Unit> {
    val userName = addedServers[serverUrl] ?: return Result.success(Unit)
    return Result.failure(AuthException.ServerAlreadyAdded(userName))
  }

  override suspend fun restore(account: BackedUpAccount, activate: Boolean): Result<User> =
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

/** Records each authorization request, then fails it as if the user backed out of the provider. */
internal class RecordingAuthorizationFlow : AuthorizationFlow {
  val requests = mutableListOf<Pair<String, Map<String, String>?>>()

  override suspend fun getAuthorization(
    serverUrl: String,
    extraHeaders: Map<String, String>?,
  ): Result<OpenIdAuthorization> {
    requests += serverUrl to extraHeaders
    return Result.failure(IllegalStateException("Cancelled"))
  }
}

internal class FakeConnectLinkInbox(
  vararg links: Pair<Long, ConnectLinkResult>,
) : ConnectLinkInbox {
  val links = mutableMapOf(*links)

  override fun deliver(id: Long, result: ConnectLinkResult): Boolean = links.put(id, result) == null
  override fun get(id: Long): ConnectLinkResult? = links[id]
  override fun discard(id: Long) {
    links.remove(id)
  }
}

internal class FakeQrCodeScanner(override val isSupported: Boolean) : QrCodeScanner {
  @Composable
  override fun Viewfinder(onScanned: (String) -> Unit, modifier: Modifier) = Unit
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
