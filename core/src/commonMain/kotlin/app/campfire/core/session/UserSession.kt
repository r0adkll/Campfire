package app.campfire.core.session

import app.campfire.core.model.Server
import app.campfire.core.model.User
import app.campfire.core.model.UserId

sealed class UserSession {
  open val key: Any get() = this

  data class LoggedIn(
    val user: User,
  ) : UserSession() {
    /**
     * A unique key to use for composition purposes where we want user/serverUrl combos to only cause composition
     * but other data on a `User` object to not.
     *
     * We could probably use another representation of the user here, like a LightUser, or something to provide the
     * specific VIEW of the data we want, but for now we will use this
     */
    override val key: Any
      get() = "${user.id}::${user.serverUrl}"
  }

  data class NeedsAuthentication(
    val server: Server,
  ) : UserSession()

  data object LoggedOut : UserSession()
  data object Loading : UserSession()
}

val UserSession.serverUrl: String? get() = when (this) {
  is UserSession.LoggedIn -> user.serverUrl
  else -> null
}

val UserSession.requireServerUrl: String get() = requireNotNull(serverUrl)

val UserSession.userId: UserId? get() = when (this) {
  is UserSession.LoggedIn -> user.id
  else -> null
}

val UserSession.requiredUserId: UserId get() = requireNotNull(userId)

val UserSession.user: User? get() = when (this) {
  is UserSession.LoggedIn -> user
  else -> null
}

val UserSession.requiredUser: User get() = requireNotNull(user)

val UserSession.isLoggedIn: Boolean
  get() = this is UserSession.LoggedIn
