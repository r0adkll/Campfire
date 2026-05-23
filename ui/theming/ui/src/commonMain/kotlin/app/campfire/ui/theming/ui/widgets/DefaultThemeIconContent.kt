package app.campfire.ui.theming.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.ConnectionIndicator
import app.campfire.common.compose.widgets.ConnectionState
import app.campfire.core.di.AppScope
import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.AppThemeRepository
import app.campfire.ui.theming.api.widgets.ThemeIconContent
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

@ContributesBinding(AppScope::class)
@Inject
class DefaultThemeIconContent(
  private val themeRepository: AppThemeRepository,
  private val socketManager: SocketManager,
) : ThemeIconContent {

  @Composable
  override fun Content(onClick: () -> Unit, modifier: Modifier) {
    val currentAppTheme by remember {
      themeRepository.observeCurrentAppTheme()
    }.collectAsState()

    val socketState by socketManager.state.collectAsState()

    Box(
      modifier = modifier,
    ) {
      AppThemeImage(
        appTheme = currentAppTheme,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onClick)
          .fillMaxSize(),
      )

      if (socketState !is SocketState.Disabled) {
        ConnectionIndicator(
          state = when (socketState) {
            is SocketState.Authenticated -> ConnectionState.Connected
            SocketState.Authenticating -> ConnectionState.Connecting
            SocketState.Connecting -> ConnectionState.Connecting
            SocketState.Disconnected -> ConnectionState.Disconnected
            is SocketState.Failed -> ConnectionState.Disconnected
            SocketState.Disabled -> error("guarded above")
          },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(
              top = 2.dp,
              end = 2.dp,
            ),
        )
      }
    }
  }
}
