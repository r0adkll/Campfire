package app.campfire.account.session

import app.campfire.account.api.UserSessionManager
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.logging.LogRedaction
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultUserSessionManager : UserSessionManager {

  private val userSessionFlow = MutableStateFlow<UserSession>(UserSession.Loading)

  override var current: UserSession
    get() = userSessionFlow.value
    set(value) {
      // Every session change (startup restore, login, account switch) funnels through
      // here, so this keeps the log redactor aware of all active server URLs.
      value.serverUrl?.let { LogRedaction.registerServerUrl(it) }
      if (value is UserSession.NeedsAuthentication) {
        LogRedaction.registerServerUrl(value.server.url)
      }
      userSessionFlow.value = value
    }

  override fun observe(): StateFlow<UserSession> {
    return userSessionFlow.asStateFlow()
  }
}
