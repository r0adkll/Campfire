package app.campfire.account

import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.account.server.LogoutUseCase
import app.campfire.account.server.SessionStopUseCase
import app.campfire.account.storage.TokenStorage
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.AppScope
import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId
import app.campfire.core.session.UserSession
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultAccountManager(
  private val settings: CampfireSettings,
  private val userSessionManager: UserSessionManager,
  private val tokenStorage: TokenStorage,
  private val logoutUseCase: LogoutUseCase,
  private val sessionStopUseCase: SessionStopUseCase,
  private val serverRepository: ServerRepository,
) : AccountManager {

  override suspend fun addAccount(serverUrl: String, token: String, user: User) {
    // Store the access token
    tokenStorage.put(user.id, token)

    // Update the stored current user id. This value is used to
    // reconstruct the session on new app launches
    settings.currentUserId = user.id

    // Set the current user session to the new user
    userSessionManager.current = UserSession.LoggedIn(user)
  }

  override suspend fun switchAccount(user: User) {
    // If we are switching to the current account, just ignore the action
    if (user.id == settings.currentUserId) return

    // Validate that we have a stored token for this user
    checkNotNull(tokenStorage.get(user.id)) { "There is no account for ${user.name}" }

    // Stop any current sessions and remove them
    sessionStopUseCase.execute()

    // Update the settings for session restoration
    settings.currentUserId = user.id

    // Update the session manager
    userSessionManager.current = UserSession.LoggedIn(user)
  }

  override suspend fun logout(server: Server) {
    val isCurrent = settings.currentUserId == server.user.id
    if (isCurrent) {
      // Stop any current sessions and remove them
      sessionStopUseCase.execute()

      // Pre-emptively switch the account over as deleting it would create a less
      // then ideal experience for users
      val remainingAccounts = serverRepository.getAllServers()
        .filter { it.user.id != server.user.id }

      if (remainingAccounts.isEmpty()) {
        // No more account put app into logged out state
        settings.currentUserId = null
        userSessionManager.current = UserSession.LoggedOut
      } else {
        val newAccount = remainingAccounts.first()
        settings.currentUserId = newAccount.user.id
        userSessionManager.current = UserSession.LoggedIn(newAccount.user)
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
}
