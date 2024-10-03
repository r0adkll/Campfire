package app.campfire.sessions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.sessions.api.SessionsRepository
import app.campfire.sessions.api.models.Session
import com.r0adkll.kimchi.annotations.ContributesTo

@ContributesTo(UserScope::class)
interface SessionHostComponent {
  val sessionsRepository: SessionsRepository
}

@Composable
private fun rememberSessionHostComponent(): SessionHostComponent {
  return remember { ComponentHolder.component() }
}

@Composable
fun SessionHostLayout(
  component: SessionHostComponent = rememberSessionHostComponent(),
  content: @Composable (Session?) -> Unit,
) {
  val currentSession by remember {
    component.sessionsRepository.observeCurrentSession()
  }.collectAsState(null)

  content(currentSession)
}
