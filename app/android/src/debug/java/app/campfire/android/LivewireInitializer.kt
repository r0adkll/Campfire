// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import androidx.media3.exoplayer.offline.DownloadManager
import app.campfire.analytics.Analytics
import app.campfire.android.plugin.CampfireAudioPlayerDebugHooks
import app.campfire.android.plugin.analytics.LivewireAnalytics
import app.campfire.android.plugin.campfire.SocketDebugCollector
import app.campfire.android.plugin.playback.DownloadDebugCollector
import app.campfire.audioplayer.impl.AudioPlayerDebugHooks
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.socket.SocketManager
import com.livewire.client.LivewireClient
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class LivewireInitializer(
  private val livewireClient: LivewireClient,
  private val downloadManager: DownloadManager,
  private val socketManager: SocketManager,
  @ForScope(AppScope::class) private val scope: CoroutineScope,
) : AppInitializer {

  override suspend fun onInitialize() {
    AudioPlayerDebugHooks.Holder.hooks = CampfireAudioPlayerDebugHooks
    DownloadDebugCollector.attach(downloadManager)
    SocketDebugCollector.attach(socketManager, scope)
    Analytics.Delegator += LivewireAnalytics
    livewireClient.start()
  }
}
