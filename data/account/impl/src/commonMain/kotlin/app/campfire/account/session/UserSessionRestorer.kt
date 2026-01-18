package app.campfire.account.session

import app.campfire.CampfireDatabase
import app.campfire.account.api.AbsToken
import app.campfire.account.api.AccountManager
import app.campfire.account.server.db.ServerWithUser
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.session.UserSession
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.settings.api.CampfireSettings
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

interface UserSessionRestorer {

  suspend fun restore(): UserSession
}

@ContributesBinding(AppScope::class)
@Inject
class DatabaseUserSessionRestorer(
  private val api: AuthAudioBookShelfApi,
  private val accountManager: AccountManager,
  private val settings: CampfireSettings,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) : UserSessionRestorer {

  override suspend fun restore(): UserSession = measureTimedValue {
    val currentUserId = settings.currentUserId ?: return@measureTimedValue UserSession.LoggedOut

    val server = withContext(dispatcherProvider.databaseRead) {
      db.serversQueries.selectByUserId(currentUserId, ::ServerWithUser)
        .awaitAsOneOrNull()
        ?.asDomainModel()
    }

    if (server == null) {
      settings.currentUserId = null
      return@measureTimedValue UserSession.LoggedOut
    }

    // Check if user is using a legacy auth token, and migrate it
    val legacyToken = accountManager.getLegacyToken(server.user.id)
    if (legacyToken != null) {
      // Timebox this request, otherwise the loading time out in the session switcher UI will crash at 5s
      withTimeoutOrNull(3.seconds) {
        val response = api.authorize(
          serverUrl = server.url,
          legacyToken = legacyToken,
        )

        if (response.isSuccess) {
          val newToken = response.getOrNull()?.user?.let { user ->
            user.accessToken?.let { accessToken -> AbsToken(accessToken, user.refreshToken) }
          }

          if (newToken != null) {
            accountManager.updateToken(
              userId = server.user.id,
              newToken = newToken,
            )
          } else {
            bark(LogPriority.ERROR) { "Failed to migrate legacy authentication token, unable to find new token" }
          }
        } else {
          bark(LogPriority.ERROR) { "Failed to migrate legacy authentication token, request failed" }
        }
      }

      // Regardless of whether or not we can migrate the token, we need to remove the legacy token
      // and let the system fallback to the re-authentication flow.
      accountManager.removeLegacyToken(server.user.id)
    }

    // Validate that this account has valid credentials
    val tokens = accountManager.getToken(server.user.id)
    if (tokens != null) {
      return@measureTimedValue UserSession.LoggedIn(server.user)
    } else {
      return@measureTimedValue UserSession.NeedsAuthentication(server)
    }
  }.let { timedValue ->
    bark("UserSessionRestorer") {
      "Restored ${timedValue.value::class.simpleName} in ${timedValue.duration}"
    }
    timedValue.value
  }
}
