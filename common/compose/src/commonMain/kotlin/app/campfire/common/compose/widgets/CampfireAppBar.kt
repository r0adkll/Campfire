package app.campfire.common.compose.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.icon
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Connected
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Connecting
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.Disconnected
import app.campfire.common.compose.widgets.AppBarState.ConnectionState.None
import app.campfire.core.model.Library
import app.campfire.core.model.Server

data class AppBarState(
  val library: LibraryState,
  val server: ServerState,
  val allLibraries: List<Library>,
  val eventSink: (AppBarViewEvent) -> Unit,
) {
  sealed interface LibraryState {
    data object Loading : LibraryState
    data class Loaded(val library: Library) : LibraryState
  }

  sealed interface ServerState {
    data object Loading : ServerState
    data class Loaded(
      val server: Server,
      val connectionState: ConnectionState,
    ) : ServerState
  }

  enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    None,
  }
}

sealed interface AppBarViewEvent {
  data class LibrarySelected(val library: Library) : AppBarViewEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampfireAppBar(
  state: AppBarState,
  onNavigationClick: () -> Unit,
  onSearchClick: () -> Unit,
  onTitleClick: () -> Unit,
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  CampfireAppBar(
    title = {
      when (state.library) {
        AppBarState.LibraryState.Loading -> CircularProgressIndicator(Modifier.size(16.dp))
        is AppBarState.LibraryState.Loaded -> {
          val canTitleClick = state.allLibraries.isNotEmpty()
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .height(TopAppBarDefaults.TopAppBarExpandedHeight)
              .padding(vertical = 12.dp, horizontal = 8.dp)
              .clip(CircleShape)
              .clickable(
                enabled = canTitleClick,
                onClick = onTitleClick,
              ),
          ) {
            Spacer(Modifier.size(24.dp))
            Text(state.library.library.name)
            if (canTitleClick) {
              Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
              )
            } else {
              Spacer(Modifier.size(24.dp))
            }
          }
        }
      }
    },
    navigationIcon = {
      ServerIcon(
        serverState = state.server,
        onClick = onNavigationClick,
      )
    },
    onSearchClick = onSearchClick,
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

/**
 * The root appbar for top-level screens to re-use to provide a consistent UI experience across
 * their surfaces.
 * @param title a composable slot to provide the title for the bar
 * @param navigationIcon a composable slot to provide the navigation/tent icon
 * @param onSearchClick called when the user clicks the search action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampfireAppBar(
  title: @Composable () -> Unit,
  navigationIcon: @Composable () -> Unit,
  onSearchClick: () -> Unit,
  modifier: Modifier = Modifier,
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  CenterAlignedTopAppBar(
    title = {
      ProvideTextStyle(
        LocalTextStyle.current.merge(fontFamily = PaytoneOneFontFamily),
      ) {
        title()
      }
    },
    navigationIcon = navigationIcon,
    actions = {
      IconButton(
        onClick = onSearchClick,
      ) {
        Icon(
          Icons.Rounded.Search,
          contentDescription = null,
        )
      }
    },
    modifier = modifier,
    scrollBehavior = scrollBehavior,
  )
}

@Composable
private fun ServerIcon(
  serverState: AppBarState.ServerState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = modifier
      .padding(start = 8.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick),
  ) {
    when (serverState) {
      AppBarState.ServerState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
      is AppBarState.ServerState.Loaded -> {
        Image(
          serverState.server.tent.icon,
          contentDescription = null,
          modifier = Modifier.size(32.dp),
        )
        if (serverState.connectionState != None) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(
                when (serverState.connectionState) {
                  Disconnected -> MaterialTheme.colorScheme.error
                  Connecting -> Color.Yellow
                  Connected -> Color.Green
                  None -> Color.Transparent
                },
                CircleShape,
              ),
          )
        }
      }
    }
  }
}
