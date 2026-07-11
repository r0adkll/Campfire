package app.campfire.android

import androidx.media3.exoplayer.offline.DownloadManager
import app.campfire.android.plugin.CampfireAudioPlayerDebugHooks
import app.campfire.android.plugin.playback.DownloadDebugCollector
import app.campfire.audioplayer.impl.AudioPlayerDebugHooks
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.livewire.client.LivewireClient
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class LivewireInitializer(
  private val livewireClient: LivewireClient,
  private val downloadManager: DownloadManager,
) : AppInitializer {

  override suspend fun onInitialize() {
    AudioPlayerDebugHooks.Holder.hooks = CampfireAudioPlayerDebugHooks
    DownloadDebugCollector.attach(downloadManager)
    livewireClient.start()
  }
}
