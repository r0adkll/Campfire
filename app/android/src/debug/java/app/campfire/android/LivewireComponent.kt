package app.campfire.android

import android.app.Application
import androidx.media3.exoplayer.offline.DownloadManager
import app.campfire.android.plugin.CoilDebugArtworkLoader
import app.campfire.android.plugin.analytics.AnalyticsLivewirePlugin
import app.campfire.android.plugin.campfire.CampfireLivewirePlugin
import app.campfire.android.plugin.playback.PlaybackLivewirePlugin
import app.campfire.audioplayer.impl.AudioPlayerService
import app.campfire.audioplayer.impl.offline.CampfireDownloadService
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.livewire.client.LivewireClient
import com.livewire.plugin.database.DatabasePlugin
import com.livewire.plugin.network.NetworkPlugin
import com.livewire.plugin.recomposition.RecompositionPlugin
import com.r0adkll.kimchi.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface LivewireComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideLivewireClient(
    application: Application,
    campfirePlugin: CampfireLivewirePlugin,
    downloadManager: DownloadManager,
  ): LivewireClient {
    return LivewireClient {
//      theme(
//        LivewireTheme(
//          AppTheme.Fixed.Tent.colorPalette.lightColorScheme,
//          AppTheme.Fixed.Tent.colorPalette.darkColorScheme,
//        ),
//      )

      install(DatabasePlugin(application))
      install(NetworkPlugin())
      install(RecompositionPlugin())
      install(campfirePlugin)
      install(AnalyticsLivewirePlugin())

      // The playback plugin is app-agnostic and destined for the Livewire library —
      // it takes its minimum needs explicitly rather than participating in our DI.
      install(
        PlaybackLivewirePlugin(
          context = application,
          sessionServiceClass = AudioPlayerService::class.java,
          downloadManager = downloadManager,
          downloadServiceClass = CampfireDownloadService::class.java,
          artworkLoader = CoilDebugArtworkLoader(application),
        ),
      )

      debugLogging(true)
    }
  }
}
