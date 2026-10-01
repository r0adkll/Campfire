// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.di

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.ktor.KtorDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import app.campfire.audioplayer.impl.networking.CampfireLoadErrorHandlingPolicy
import app.campfire.audioplayer.impl.offline.downloadRequirements
import app.campfire.core.di.AppScope
import app.campfire.network.di.AudioPlayerClient
import app.campfire.settings.api.MobileDataSettings
import app.campfire.settings.api.PlaybackSettings
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import java.io.File
import java.util.concurrent.Executors

@ContributesTo(AppScope::class)
interface ExoPlayerAppComponent {

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideDatabaseProvider(
    application: Application,
  ): DatabaseProvider = StandaloneDatabaseProvider(application)

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  @DownloadCache
  fun provideDownloadCache(
    application: Application,
    databaseProvider: DatabaseProvider,
  ): SimpleCache {
    val downloadDirectory = File(application.filesDir, "exoPlayerDownloads")
    return SimpleCache(
      downloadDirectory,
      // Downloads must never be evicted, otherwise downloaded items silently lose their bytes
      // and offline playback falls through to the network.
      NoOpCacheEvictor(),
      databaseProvider,
    )
  }

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  @StreamingCache
  fun provideStreamingCache(
    application: Application,
    databaseProvider: DatabaseProvider,
  ): SimpleCache {
    val streamingCacheDirectory = File(application.cacheDir, "exoPlayerStreamingCache")
    return SimpleCache(
      streamingCacheDirectory,
      LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024L), // 512 MB disk cap
      databaseProvider,
    )
  }

  /**
   * Media requests go through the [AudioPlayerClient], which authenticates with the same bearer
   * provider as the user client: it attaches the session user's token and extra headers, and a
   * 401 refreshes the shared token and retries before the player ever sees it.
   */
  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideHttpDataSourceFactory(
    @AudioPlayerClient client: HttpClient,
  ): HttpDataSource.Factory = KtorDataSource.Factory(client)

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideExoPlayerDownloadManager(
    application: Application,
    databaseProvider: DatabaseProvider,
    @DownloadCache downloadCache: SimpleCache,
    httpDataSourceFactory: HttpDataSource.Factory,
    mobileDataSettings: MobileDataSettings,
  ): DownloadManager {
    val numCpus = Runtime.getRuntime().availableProcessors()
    return DownloadManager(
      application,
      databaseProvider,
      downloadCache,
      httpDataSourceFactory,
      Executors.newFixedThreadPool(numCpus),
    ).apply {
      maxParallelDownloads = numCpus
      // Set on the manager's own thread; later changes go through DownloadRequirementsObserver
      requirements = downloadRequirements(wifiOnly = mobileDataSettings.downloadOnWifiOnly)
    }
  }

  @OptIn(UnstableApi::class)
  @Provides
  fun provideMediaSourceFactory(
    application: Application,
    settings: PlaybackSettings,
    @DownloadCache downloadCache: SimpleCache,
    @StreamingCache streamingCache: SimpleCache,
    httpDataSourceFactory: HttpDataSource.Factory,
    loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
  ): MediaSource.Factory {
    val streamingCacheDataSourceFactory = CacheDataSource.Factory()
      .setCache(streamingCache)
      .setUpstreamDataSourceFactory(httpDataSourceFactory)

    val cacheDataSourceFactory = CacheDataSource.Factory()
      .setCache(downloadCache)
      // Read-only: only the DownloadManager writes into the download cache. Streamed
      // (non-downloaded) content is cached in the size-capped streaming cache instead.
      .setCacheWriteDataSinkFactory(null)
      .setUpstreamDataSourceFactory(streamingCacheDataSourceFactory)

    val extractorsFactory = DefaultExtractorsFactory()

    if (settings.enableMp3IndexSeeking) {
      // https://exoplayer.dev/troubleshooting.html#why-is-seeking-inaccurate-in-some-mp3-files
      extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
    }

    return DefaultMediaSourceFactory(application, extractorsFactory)
      .setDataSourceFactory(cacheDataSourceFactory)
      .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
  }

  @OptIn(UnstableApi::class)
  @Provides
  fun provideLoadErrorHandlingPolicy(): LoadErrorHandlingPolicy = CampfireLoadErrorHandlingPolicy()
}
