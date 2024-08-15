package app.campfire.core.session

sealed class UserSession {

  data class LoggedIn(
    val serverUrl: String,
  ) : UserSession()

  data object LoggedOut : UserSession()
}
