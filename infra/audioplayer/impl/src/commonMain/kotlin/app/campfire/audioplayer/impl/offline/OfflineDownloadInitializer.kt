package app.campfire.audioplayer.impl.offline

import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(AppScope::class)
@Inject
class OfflineDownloadInitializer(
  private val offlineDownloadManager: OfflineDownloadManager,
) : AppInitializer {
  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    /**
     * Make sure we resume any paused or incomplete media downloads
     */
    offlineDownloadManager.resumeDownloads()
  }
}
