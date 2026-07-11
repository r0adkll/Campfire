package app.campfire.android.plugin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.socket.SocketManager
import app.campfire.socket.SocketState
import com.livewire.ui.Plugin
import com.livewire.ui.PluginInfo
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.padding
import com.livewire.ui.widget.Text
import com.livewire.ui.widget.TextStyle
import me.tatarka.inject.annotations.Inject

@Inject
class CampfireLivewirePlugin(
  private val socketManager: SocketManager,
) : Plugin {

  override val info: PluginInfo = PluginInfo(
    pluginId = "campfire",
    title = "Campfire",
    icon = CampfireIcons.Campfire,
  )

  @Composable
  override fun Content() {
    Column(
      LivewireModifier
        .fillMaxSize(),
    ) {
      // Show the socket state
      val socketState by socketManager.state.collectAsState()
      Row(
        LivewireModifier
          .padding(16.dp),
      ) {
        when (socketState) {
          is SocketState.Authenticated -> Text(
            text = "Authenticated! [${(socketState as SocketState.Authenticated).username}]",
            color = Color.Green,
            style = TextStyle.TitleMedium,
          )
          SocketState.Authenticating -> Text(
            text = "Authenticating…",
            color = Color.Cyan,
            style = TextStyle.TitleMedium,
          )
          SocketState.Connecting -> Text(
            text = "Connecting…",
            color = Color.Yellow,
            style = TextStyle.TitleMedium,
          )
          SocketState.Disabled -> Text(
            text = "Disabled",
            color = Color.LightGray,
            style = TextStyle.TitleMedium,
          )
          SocketState.Disconnected -> Text(
            text = "Disconnected",
            color = Color.DarkGray,
            style = TextStyle.TitleMedium,
          )
          is SocketState.Failed -> Text(
            text = "Failed[${(socketState as SocketState.Failed).reason}]",
            color = Color.Red,
            style = TextStyle.TitleMedium,
          )
        }
      }
    }
  }

  companion object {
    const val ICON = "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24px\" viewBox=\"0 -960 960 960\" width=\"24px\" fill=\"#e3e3e3\"><path d=\"M253-173q-93-93-93-227 0-113 67-217t184-182q22-15 45.5-1.5T480-760v52q0 34 23.5 57t57.5 23q17 0 32.5-7.5T621-657q8-10 20.5-12.5T665-664q63 45 99 115t36 149q0 134-93 227T480-80q-134 0-227-93Zm-13-227q0 52 21 98.5t60 81.5q-1-5-1-9v-9q0-32 12-60t35-51l113-111 113 111q23 23 35 51t12 60v9q0 4-1 9 39-35 60-81.5t21-98.5q0-50-18.5-94.5T648-574q-20 13-42 19.5t-45 6.5q-62 0-107.5-41T401-690q-78 66-119.5 140.5T240-400Zm240 52-57 56q-11 11-17 25t-6 29q0 32 23.5 55t56.5 23q33 0 56.5-23t23.5-55q0-16-6-29.5T537-292l-57-56Z\"/></svg>"
  }
}
