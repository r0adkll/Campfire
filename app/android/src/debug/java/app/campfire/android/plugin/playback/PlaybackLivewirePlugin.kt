package app.campfire.android.plugin.playback

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.session.MediaSessionService
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.MotionPlay
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

/**
 * A Livewire plugin focused on debugging the media3 stack: the MediaSession and its
 * controllers, the ExoPlayer queue/metadata, and the Android Auto browse tree.
 *
 * All MediaController interaction happens through a controller owned by this plugin
 * (see [rememberDebugMediaController]) — never the app UI's MediaControllerConnector.
 */
class PlaybackLivewirePlugin(
  private val context: Context,
  private val sessionServiceClass: Class<out MediaSessionService>,
  private val downloadManager: DownloadManager,
  private val downloadServiceClass: Class<out DownloadService>,
  private val artworkLoader: DebugArtworkLoader,
) : Plugin {

  override val info: PluginInfo = PluginInfo(
    pluginId = "jetpack-media3",
    icon = CampfireIcons.Rounded.MotionPlay,
    title = "Jetpack Media3",
  )

  @Composable
  override fun Content() {
    val browser = rememberDebugMediaBrowser(context, sessionServiceClass)
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
            text = "Player",
            selected = selectedTab == 0,
            onClick = clickAction { selectedTab = 0 },
          )
          Tab(
            text = "Controllers",
            selected = selectedTab == 1,
            onClick = clickAction { selectedTab = 1 },
          )
          Tab(
            text = "Auto",
            selected = selectedTab == 2,
            onClick = clickAction { selectedTab = 2 },
          )
          Tab(
            text = "Downloads",
            selected = selectedTab == 3,
            onClick = clickAction { selectedTab = 3 },
          )
        }
      }

      when (selectedTab) {
        0 -> PlayerTab(browser, artworkLoader)
        1 -> ControllersTab()
        2 -> AutoTab(browser)
        3 -> DownloadsTab(downloadManager, downloadServiceClass, context)
      }
    }
  }
}
