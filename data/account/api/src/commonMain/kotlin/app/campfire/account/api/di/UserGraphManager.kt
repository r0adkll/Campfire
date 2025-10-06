package app.campfire.account.api.di

import app.campfire.core.session.UserSession

interface UserGraphManager {

  fun create(userSession: UserSession)
  suspend fun destroy()
}
