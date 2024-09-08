package app.campfire.common.compose.navigation

import androidx.compose.runtime.compositionLocalOf
import app.campfire.core.session.UserSession

val LocalUserSession = compositionLocalOf<UserSession> {
  error("No user session provided in this composition")
}
