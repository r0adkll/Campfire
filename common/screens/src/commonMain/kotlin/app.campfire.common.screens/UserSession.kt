package app.campfire.common.screens

import app.campfire.core.session.UserSession

/**
 * Utility extension property to associate the root screens for each [UserSession] state
 * that the user might be experiencing.
 */
val UserSession.rootScreen: BaseScreen get() = when (this) {
  is UserSession.LoggedIn -> HomeScreen
  else -> WelcomeScreen
}
