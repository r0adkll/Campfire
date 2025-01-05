package app.campfire.shared.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.navigation.LocalUserSession
import app.campfire.core.di.ComponentHolder
import app.campfire.core.session.UserSession
import app.campfire.shared.di.UserComponent
import app.campfire.shared.di.UserComponentManager
import app.campfire.shared.di.rememberUserComponentManager

sealed interface ServerUrlState {
  data object Loading : ServerUrlState
  data class Loaded(val serverUrl: String?) : ServerUrlState
}

@Composable
fun UserComponentContent(
  userSessionManager: UserSessionManager,
  userComponentManager: UserComponentManager = rememberUserComponentManager(),
  content: @Composable (UserComponent) -> Unit,
) {
  val scope = rememberCoroutineScope()

  val userSession by remember {
    userSessionManager.observe()
  }.collectAsState()

  when (userSession) {
    is UserSession.LoggedIn,
    is UserSession.LoggedOut,
    -> {
      val userComponent = remember(userSession.key) {
        // Fetch a cached graph object, or create a new one for the current session
        userComponentManager.getOrCreateUserComponent(userSession)
          .also { component ->
            // Be sure to update the current instance of [UserComponent] in the holder
            ComponentHolder.updateComponent(scope, component)
          }
      }

      CompositionLocalProvider(
        LocalUserSession provides userComponent.currentUserSession,
      ) {
        content(userComponent)
      }
    }

    else -> SplashScreen()
  }
}

@Composable
private fun SplashScreen(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      Icons.Campfire,
      contentDescription = null,
      modifier = Modifier
        .size(236.dp),
    )
  }
}
