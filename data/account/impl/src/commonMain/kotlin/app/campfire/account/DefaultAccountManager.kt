package app.campfire.account

import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.account.api.di.UserGraphManager
import app.campfire.account.server.LogoutUseCase
import app.campfire.account.storage.TokenStorage
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAccountManager(
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
  private val tokenStorage: TokenStorage,
  private val logoutUseCase: LogoutUseCase,
  private val serverRepository: ServerRepository,
  private val userGraphManager: UserGraphManager,
  @ForScope(AppScope::class) val applicationScope: CoroutineScope,
) : AccountManager {

  // Since these operations are often started from Ui's and their coroutine scope, the act of
  // changing the user session will cause the entire view composition to reset and cancel all those
  // scopes. So we need to make sure this work is performed on the application scope.
  private val accountManagerCoroutineContext = applicationScope.coroutineContext +
    CoroutineExceptionHandler { context, throwable ->
      bark(LogPriority.ERROR, throwable = throwable) { "Coroutine Exception in AccountManager" }
    }

  override suspend fun addAccount(serverUrl: String, token: String, user: User) =
    withContext(accountManagerCoroutineContext) {
      // Store the access token
      tokenStorage.put(user.id, token)

      // Check for other accounts
      val hasOtherAccounts = serverRepository.getAllServers().any { it.user.id != user.id }

      // Switch the session over
      changeSession {
        UserSession.LoggedIn(user, showAnalyticsConsent = !hasOtherAccounts)
      }
    }

  override suspend fun switchAccount(user: User) = withContext(accountManagerCoroutineContext) {
    // If we are switching to the current account, just ignore the action
    if (user.id == settings.currentUserId) return@withContext

    // Validate that we have a stored token for this user
    checkNotNull(tokenStorage.get(user.id)) { "There is no account for ${user.name}" }

    changeSession {
      // Create the new user session
      UserSession.LoggedIn(user)
    }
  }

  override suspend fun logout(server: Server) = withContext(accountManagerCoroutineContext) {
    val isCurrent = settings.currentUserId == server.user.id
    if (isCurrent) {
      // Change the session over to a new one
      changeSession {
        // Pre-emptively switch the account over as deleting it would create a less
        // then ideal experience for users
        val remainingAccounts = serverRepository.getAllServers()
          .filter { it.user.id != server.user.id }

        if (remainingAccounts.isEmpty()) {
          // No more account put app into logged out state
          UserSession.LoggedOut
        } else {
          val newAccount = remainingAccounts.first()
          UserSession.LoggedIn(newAccount.user)
        }
      }
    }

    // Delete the accounts token
    tokenStorage.remove(server.user.id)

    // Delete the accounts data
    logoutUseCase.execute(server)
  }

  override suspend fun getToken(userId: UserId): String? {
    return tokenStorage.get(userId)
  }

  private suspend inline fun changeSession(block: suspend () -> UserSession) {
    // Force the UI into a loading state, making sure to pull all usages of the current graph
    // out of composition.
    userSessionManager.current = UserSession.Loading

    // Create the new session
    val newSession = block()

    // Re-create the UserScope/UserComponent
    userGraphManager.destroy()
    userGraphManager.create(newSession)

    // Update persisted settings and observers
    settings.currentUserId = newSession.userId
    userSessionManager.current = newSession
  }
}
