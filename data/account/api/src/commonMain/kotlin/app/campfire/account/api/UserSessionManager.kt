package app.campfire.account.api

import app.campfire.core.session.UserSession
import kotlinx.coroutines.flow.StateFlow

interface UserSessionManager {

  var current: UserSession

  fun observe(): StateFlow<UserSession>
}
