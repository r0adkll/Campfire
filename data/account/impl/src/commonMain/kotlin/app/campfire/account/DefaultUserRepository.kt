package app.campfire.account

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserRepository
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.data.mapping.asDomainModel
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import me.tatarka.inject.annotations.Inject

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class DefaultUserRepository(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserRepository {

  private var cachedUser: User? = null

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeCurrentUser(): Flow<User> {
    val serverUrl = userSession.serverUrl ?: return emptyFlow()
    return db.usersQueries.selectForServer(serverUrl)
      .asFlow()
      .mapToOne(dispatcherProvider.databaseRead)
      .mapLatest {
        it.asDomainModel().also { domainUser ->
          cachedUser = domainUser
        }
      }
  }

  override suspend fun getCurrentUser(): User {
    if (cachedUser != null) return cachedUser!!
    val serverUrl = userSession.serverUrl ?: throw IllegalStateException("No server found")
    return db.usersQueries.selectForServer(serverUrl)
      .executeAsOne()
      .asDomainModel()
      .also { cachedUser = it }
  }
}
