package app.campfire.android.plugin.campfire

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import app.campfire.account.api.AccountManager
import app.campfire.account.api.ServerRepository
import app.campfire.account.api.UserSessionManager
import app.campfire.common.compose.icons.Campfire
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.network.AuthAudioBookShelfApi
import app.campfire.socket.SocketManager
import com.livewire.ui.Plugin
import com.livewire.ui.PluginInfo
import com.livewire.ui.actions.clickAction
import com.livewire.ui.layout.Column
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxSize
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.padding
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Tab
import com.livewire.ui.widget.TabRow
import me.tatarka.inject.annotations.Inject

/**
 * Campfire-specific debugging: the Audiobookshelf socket connection, the account /
 * session / token state, and current server information. Deliberately app-coupled —
 * the counterpart to the app-agnostic media3 playback plugin.
 */
@Inject
class CampfireLivewirePlugin(
  private val socketManager: SocketManager,
  private val userSessionManager: UserSessionManager,
  private val accountManager: AccountManager,
  private val serverRepository: ServerRepository,
  private val authApi: AuthAudioBookShelfApi,
) : Plugin {

  override val info: PluginInfo = PluginInfo(
    pluginId = "campfire",
    title = "Campfire",
    icon = CampfireIcons.Campfire,
  )

  @Composable
  override fun Content() {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
      LivewireModifier
        .fillMaxSize(),
    ) {
      Surface(
        modifier = LivewireModifier
          .fillMaxWidth(),
        color = LivewireTheme.colorScheme.surfaceContainer,
      ) {
        TabRow(
          modifier = LivewireModifier.padding(top = 8.dp),
        ) {
          Tab(
            text = "Socket",
            selected = selectedTab == 0,
            onClick = clickAction { selectedTab = 0 },
          )
          Tab(
            text = "Account",
            selected = selectedTab == 1,
            onClick = clickAction { selectedTab = 1 },
          )
          Tab(
            text = "Server",
            selected = selectedTab == 2,
            onClick = clickAction { selectedTab = 2 },
          )
        }
      }

      when (selectedTab) {
        0 -> SocketTab(socketManager)
        1 -> AccountTab(userSessionManager, accountManager, serverRepository)
        2 -> ServerTab(serverRepository, authApi)
      }
    }
  }
}
