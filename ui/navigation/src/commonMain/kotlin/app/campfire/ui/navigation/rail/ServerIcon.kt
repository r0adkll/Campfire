package app.campfire.ui.navigation.rail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.campfire.account.api.ServerRepository
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.widgets.AppBarState
import app.campfire.common.compose.widgets.DefaultServerIconSize
import app.campfire.common.compose.widgets.ServerIcon
import app.campfire.common.compose.widgets.ServerState
import app.campfire.core.di.AppScope
import app.campfire.settings.api.CampfireSettings
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@ContributesTo(AppScope::class)
interface ServerIconComponent {
  val settings: CampfireSettings
  val serverRepository: ServerRepository
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
internal fun ServerIcon(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = DefaultServerIconSize,
  component: ServerIconComponent = rememberComponent(),
) {
  val serverState by remember(component) {
    component.serverRepository.observeCurrentServer()
      .flatMapLatest { server ->
        component.settings.observeUseDynamicColors()
          .map { useDynamicColors ->
            ServerState.Loaded(
              server = server,
              useDynamicColors = useDynamicColors,
              connectionState = AppBarState.ConnectionState.None,
            )
          }
      }
      .catch<ServerState> { emit(ServerState.Error) }
  }.collectAsState(ServerState.Loading)

  ServerIcon(
    serverState = serverState,
    onClick = onClick,
    size = size,
    modifier = modifier,
  )
}
