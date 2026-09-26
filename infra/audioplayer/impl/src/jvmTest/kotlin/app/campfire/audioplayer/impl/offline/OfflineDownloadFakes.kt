// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import app.campfire.account.api.UserSessionManager
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal val downloadingUser = User(
  id = "user-1",
  name = "Listener",
  selectedLibraryId = "lib-1",
  type = User.Type.User,
  isActive = true,
  isLocked = false,
  lastSeen = 0L,
  createdAt = 0L,
  permissions = User.Permissions(
    download = true,
    update = false,
    delete = false,
    upload = false,
    accessAllLibraries = true,
    accessAllTags = true,
    accessExplicitContent = true,
  ),
  serverUrl = "https://abs.example.com",
)

internal class FakeAccountManager : AccountManager {
  var token: AbsToken? = null
  var extraHeaders: Map<String, String>? = null

  override suspend fun addAccount(
    serverUrl: String,
    accessToken: String,
    refreshToken: String?,
    extraHeaders: Map<String, String>?,
    user: User,
  ) = Unit

  override suspend fun invalidateAccount(user: User) = Unit

  override suspend fun switchAccount(user: User) = Unit

  override suspend fun logout(server: Server) = Unit

  override suspend fun getToken(userId: UserId): AbsToken? = token

  override suspend fun updateToken(userId: UserId, newToken: AbsToken) {
    token = newToken
  }

  override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = extraHeaders
  override suspend fun setExtraHeaders(userId: UserId, headers: Map<String, String>) = Unit
  override fun observeExtraHeaders(userId: UserId): Flow<Map<String, String>> = emptyFlow()
}

internal class FakeTokenRefresher(private val accountManager: FakeAccountManager) : TokenRefresher {
  var next: AbsToken? = null
  val refreshedWith = mutableListOf<String?>()

  override suspend fun refresh(userId: UserId, serverUrl: String, staleAccessToken: String?): AbsToken? {
    refreshedWith += staleAccessToken
    return next?.also { accountManager.token = it }
  }
}

internal class FakeUserSessionManager(session: UserSession) : UserSessionManager {
  private val state = MutableStateFlow(session)

  override var current: UserSession
    get() = state.value
    set(value) {
      state.value = value
    }

  override fun observe(): StateFlow<UserSession> = state
}
