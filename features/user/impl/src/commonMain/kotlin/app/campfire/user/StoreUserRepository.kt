// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user

import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUser
import app.campfire.user.api.UserRepository
import app.campfire.user.store.UserStore
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.impl.extensions.get

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreUserRepository(
  private val userSession: UserSession,
  private val userStoreFactory: UserStore.Factory,
  @ForScope(UserScope::class) private val coroutineScopeHolder: CoroutineScopeHolder,
) : UserRepository {

  private val userStore by lazy { userStoreFactory.create() }

  override val userFlow: StateFlow<User> by lazy {
    userStore.stream(StoreReadRequest.cached(Unit, refresh = false))
      .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
      .map { it.requireData() }
      .distinctUntilChanged()
      .stateIn(
        // Cache this in our UserScope coroutine scope
        scope = coroutineScopeHolder.get(),
        started = SharingStarted.Eagerly,
        initialValue = userSession.requiredUser,
      )
  }

  override fun observeCurrentUser(): Flow<User> {
    if (userSession is UserSession.LoggedOut) return emptyFlow()
    return userStore.stream(StoreReadRequest.cached(Unit, refresh = false))
      .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
      .map { it.requireData() }
      .distinctUntilChanged()
  }

  override suspend fun getCurrentUser(): User {
    return userStore.get(Unit)
  }
}
