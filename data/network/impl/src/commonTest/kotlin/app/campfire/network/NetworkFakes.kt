// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal val testUser = User(
  id = "user-1",
  name = "Testy McTestface",
  selectedLibraryId = "lib-1",
  type = User.Type.User,
  isActive = true,
  isLocked = false,
  lastSeen = 0L,
  createdAt = 0L,
  permissions = User.Permissions(
    download = true,
    update = true,
    delete = true,
    upload = true,
    accessAllLibraries = true,
    accessAllTags = true,
    accessExplicitContent = true,
  ),
  serverUrl = "https://abs.example.com",
)

internal class FakeAccountManager : AccountManager {
  val tokens = mutableMapOf<UserId, AbsToken>()
  var extraHeaders: Map<String, String>? = null
  var invalidatedUser: User? = null

  override suspend fun addAccount(
    serverUrl: String,
    accessToken: String,
    refreshToken: String?,
    extraHeaders: Map<String, String>?,
    user: User,
  ) {
    tokens[user.id] = AbsToken(accessToken, refreshToken)
  }

  override suspend fun invalidateAccount(user: User) {
    invalidatedUser = user
    tokens.remove(user.id)
  }

  override suspend fun switchAccount(user: User) = Unit

  override suspend fun logout(server: Server) = Unit

  override suspend fun getToken(userId: UserId): AbsToken? = tokens[userId]

  override suspend fun updateToken(userId: UserId, newToken: AbsToken) {
    tokens[userId] = newToken
  }

  override suspend fun getExtraHeaders(userId: UserId): Map<String, String>? = extraHeaders
  override suspend fun setExtraHeaders(userId: UserId, headers: Map<String, String>) = Unit
  override fun observeExtraHeaders(userId: UserId): Flow<Map<String, String>> = emptyFlow()
}

internal class FakeUserSessionManager(initial: UserSession) : UserSessionManager {
  private val state = MutableStateFlow(initial)

  override var current: UserSession
    get() = state.value
    set(value) {
      state.value = value
    }

  override fun observe(): StateFlow<UserSession> = state
}
