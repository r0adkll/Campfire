package app.campfire.core.session

sealed class UserSession {

  abstract val serverUrl: String?

  data class LoggedIn(
    override val serverUrl: String,
  ) : UserSession()

  data object LoggedOut : UserSession() {
    override val serverUrl: String? = null
  }
}

val UserSession.isLoggedIn: Boolean
  get() = this is UserSession.LoggedIn

val UserSession.requiredServerUrl: String
  get() = serverUrl!!
