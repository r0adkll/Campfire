package app.campfire.audioplayer.impl.di

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.audioplayer.impl.networking.AuthRefreshingHttpDataSource
import app.campfire.audioplayer.impl.networking.CampfireLoadErrorHandlingPolicy
import app.campfire.core.app.ApplicationInfo
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.session.UserSession
import app.campfire.core.session.requiredUserId
import app.campfire.network.di.UserClient
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesTo
import io.ktor.client.HttpClient
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Provides

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

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideExoPlayerDownloadManager(
    application: Application,
    sessionManager: UserSessionManager,
    accountManager: AccountManager,
    databaseProvider: DatabaseProvider,
    @DownloadCache downloadCache: SimpleCache,
    appInfo: ApplicationInfo,
    @UserClient userClient: HttpClient,
  ): DownloadManager {
    val numCpus = Runtime.getRuntime().availableProcessors()
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
      .setUserAgent(appInfo.userAgent)
    val authRetryFactory = AuthRefreshingHttpDataSource.Factory(httpDataSourceFactory, userClient)
    return DownloadManager(
      application,
      databaseProvider,
      downloadCache,
      createAuthenticatingDataSource(
        userSessionProvider = { sessionManager.current },
        accountManager = accountManager,
        upstreamDataSourceFactory = authRetryFactory,
      ),
      Executors.newFixedThreadPool(numCpus),
    ).apply {
      maxParallelDownloads = numCpus
    }
  }

  @OptIn(UnstableApi::class)
  @Provides
  fun provideMediaSourceFactory(
    application: Application,
    settings: PlaybackSettings,
    @DownloadCache downloadCache: SimpleCache,
    @StreamingCache streamingCache: SimpleCache,
    sessionManager: UserSessionManager,
    accountManager: AccountManager,
    appInfo: ApplicationInfo,
    loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    @UserClient userClient: HttpClient,
  ): MediaSource.Factory {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
      .setUserAgent(appInfo.userAgent)

    val authRetryFactory = AuthRefreshingHttpDataSource.Factory(httpDataSourceFactory, userClient)

    val streamingCacheDataSourceFactory = CacheDataSource.Factory()
      .setCache(streamingCache)
      .setUpstreamDataSourceFactory(authRetryFactory)

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
      .setDataSourceFactory(
        createAuthenticatingDataSource(
          userSessionProvider = { sessionManager.current },
          accountManager = accountManager,
          upstreamDataSourceFactory = cacheDataSourceFactory,
        ),
      )
      .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
  }

  @OptIn(UnstableApi::class)
  @Provides
  fun provideLoadErrorHandlingPolicy(): LoadErrorHandlingPolicy = CampfireLoadErrorHandlingPolicy()
}

@OptIn(UnstableApi::class)
private fun createAuthenticatingDataSource(
  userSessionProvider: () -> UserSession,
  accountManager: AccountManager,
  upstreamDataSourceFactory: DataSource.Factory,
) = ResolvingDataSource.Factory(upstreamDataSourceFactory) { dataSpec ->
  // ⚠️🐲 DRAGONS BE HERE! Using runBlocking in production can be prone to foot guns, especially when
  //  executed in other coroutine contexts. Since this is used internally by ExoPlayer we should be
  //  safe from that particular issue.
  val token = runBlocking {
    accountManager.getToken(userSessionProvider().requiredUserId)
  }

  val extraHeaders = runBlocking {
    accountManager.getExtraHeaders(userSessionProvider().requiredUserId)
  } ?: emptyMap()

  if (token != null) {
    dataSpec
      .withAdditionalHeaders(mapOf("Authorization" to "Bearer ${token.accessToken}"))
      .withAdditionalHeaders(extraHeaders)
  } else {
    dataSpec
  }
}
