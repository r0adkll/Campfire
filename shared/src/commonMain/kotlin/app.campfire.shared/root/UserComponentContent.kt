package app.campfire.shared.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.di.ComponentHolder
import app.campfire.core.session.UserSession
import app.campfire.shared.di.UserComponent
import kotlinx.coroutines.flow.map

sealed interface ServerUrlState {
  data object Loading : ServerUrlState
  data class Loaded(val serverUrl: String?) : ServerUrlState
}

@Composable
fun UserComponentContent(
  campfireSettings: CampfireSettings,
  content: @Composable (UserComponent) -> Unit,
) {
  val serverUrlState by campfireSettings.observeCurrentServerUrl()
    .map { ServerUrlState.Loaded(it) }
    .collectAsState(ServerUrlState.Loading)

  when (val state = serverUrlState) {
    is ServerUrlState.Loaded -> {
      val currentServerUrl = state.serverUrl

      val userComponent = remember(currentServerUrl) {
        ComponentHolder.component<UserComponent.Factory>()
          .create(
            currentServerUrl
              ?.let { UserSession.LoggedIn(it) }
              ?: UserSession.LoggedOut,
          )
      }

      content(userComponent)
    }

    ServerUrlState.Loading -> SplashScreen()
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
