package app.campfire.auth

import app.campfire.CampfireDatabase
import app.campfire.auth.api.AuthRepository
import app.campfire.auth.mapping.asDatabaseModel
import app.campfire.core.di.AppScope
import app.campfire.core.model.Tent
import app.campfire.network.AudioBookShelfApi
import com.r0adkll.kotlininject.merge.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAuthRepository(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
) : AuthRepository {

  override suspend fun ping(serverUrl: String): Boolean {
    return api.ping(serverUrl)
  }

  override suspend fun authenticate(
    serverUrl: String,
    serverName: String,
    username: String,
    password: String,
    tent: Tent,
  ) : Result<Unit> {
    val result = api.login(serverUrl, username, password)

    if (result.isSuccess) {
      val response = result.getOrThrow()

      // Insert Server & User
      // This action should prompt observables to see the new server/user and update
      // the UI accordingly
      db.transaction {
        db.serversQueries.insert(
          response.serverSettings.asDatabaseModel(
            url = serverUrl,
            name = serverName,
            tent = tent,
          )
        )

        // Insert User
        db.usersQueries.insert(
          response.user.asDatabaseModel(serverUrl)
        )
      }

      return Result.success(Unit)
    } else {
      return result.map { Unit }
    }
  }
}
