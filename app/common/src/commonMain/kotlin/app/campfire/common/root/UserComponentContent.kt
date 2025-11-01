package app.campfire.common.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.navigation.LocalUserSession
import app.campfire.common.di.UserComponent
import app.campfire.core.di.ComponentHolder
import app.campfire.core.session.UserSession
import kotlinx.coroutines.delay

@Composable
fun UserComponentContent(
  userSessionManager: UserSessionManager,
  content: @Composable (UserComponent) -> Unit,
) {
  val userSession by remember {
    userSessionManager.observe()
  }.collectAsState()

  when (userSession) {
    is UserSession.LoggedOut,
    is UserSession.LoggedIn,
    -> {
      val userComponent = remember(userSession) {
        ComponentHolder.component<UserComponent>()
      }

      CompositionLocalProvider(
        LocalUserSession provides userSession,
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
  LaunchedEffect(Unit) {
    delay(5_000L)
    error("Splash screen timed out. It's taking too long to load the main UI(s)")
  }

  Box(
    modifier = modifier
      .fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      CampfireIcons.Campfire,
      contentDescription = null,
      modifier = Modifier
        .size(236.dp),
    )
  }
}
