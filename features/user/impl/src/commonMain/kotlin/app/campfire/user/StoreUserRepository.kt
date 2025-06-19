package app.campfire.user

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.User
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.asFetcherResult
import app.campfire.network.AudioBookShelfApi
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.impl.extensions.get

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StoreUserRepository(
  private val userSession: UserSession,
  private val db: CampfireDatabase,
  private val api: AudioBookShelfApi,
  private val dispatcherProvider: DispatcherProvider,
) : UserRepository {

  private val userStore = StoreBuilder.from(
    fetcher = Fetcher.ofResult { _: Unit -> api.getCurrentUser().asFetcherResult() },
    sourceOfTruth = SourceOfTruth.of(
      reader = {
        if (userSession.serverUrl == null) return@of flowOf(null)
        db.usersQueries.selectForServer(userSession.serverUrl!!)
          .asFlow()
          .mapToOneOrNull(dispatcherProvider.databaseRead)
          .map { it?.asDomainModel() }
      },
      writer = { _, networkUser ->
        // The initial user should already be in the database, so we should ONLY update it
        withContext(dispatcherProvider.databaseWrite) {
          db.usersQueries.update(
            name = networkUser.username,
            type = User.Type.from(networkUser.type),
            seriesHideFromContinueListening = networkUser.seriesHideFromContinueListening,
            isActive = networkUser.isActive,
            isLocked = networkUser.isLocked,
            lastSeen = networkUser.lastSeen,
            createdAt = networkUser.createdAt,
            permission_download = networkUser.permissions.download,
            permission_upload = networkUser.permissions.upload,
            permission_delete = networkUser.permissions.delete,
            permission_update = networkUser.permissions.update,
            permission_accessAllLibraries = networkUser.permissions.accessAllLibraries,
            permission_accessExplicitContent = networkUser.permissions.accessExplicitContent,
            permission_accessAllTags = networkUser.permissions.accessAllTags,
            librariesAccessible = networkUser.librariesAccessible,
            itemTagsAccessible = networkUser.itemTagsAccessible ?: emptyList(),
            id = networkUser.id,
          )
        }
      },
    ),
  ).cachePolicy(
    MemoryPolicy.builder<Any, User>()
      .setMaxSize(1)
      .build(),
  ).build()

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
