package app.campfire.audioplayer.impl.di

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesTo
import java.io.File
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface ExoPlayerComponent {

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideDatabaseProvider(
    application: Application,
  ): DatabaseProvider = StandaloneDatabaseProvider(application)

  @OptIn(UnstableApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideSimpleCache(
    application: Application,
    databaseProvider: DatabaseProvider,
  ): SimpleCache {
    val downloadDirectory = File(application.filesDir, "exoPlayerDownloads")
    return SimpleCache(
      downloadDirectory,
      NoOpCacheEvictor(),
      databaseProvider,
    )
  }

  @OptIn(UnstableApi::class)
  @Provides
  fun provideMediaSourceFactory(
    application: Application,
    settings: PlaybackSettings,
    simpleCache: SimpleCache,
//    userSession: UserSession,
//    accountManager: AccountManager,
  ): MediaSource.Factory {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()

    // Configure the DataSource.Factory with the cache and factory for the desired HTTP stack.
    val cacheDataSourceFactory =
      CacheDataSource.Factory()
        .setCache(simpleCache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)

    val extractorsFactory = DefaultExtractorsFactory()

    if (settings.enableMp3IndexSeeking) {
      // https://exoplayer.dev/troubleshooting.html#why-is-seeking-inaccurate-in-some-mp3-files
      extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
    }

    return DefaultMediaSourceFactory(application, extractorsFactory)
      .setDataSourceFactory(cacheDataSourceFactory)
//      .setDataSourceFactory(
//        ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
//          // ⚠️ DRAGONS BE HERE! Using runBlocking in production can be prone to foot guns, especially when
//          //  executed in other coroutine contexts. Since this is used internally by ExoPlayer we should be
//          //  safe from that particular issue.
//          val token = runBlocking {
//            accountManager.getToken(userSession.requiredServerUrl)
//          }
//
//          if (token != null) {
//            dataSpec.withAdditionalHeaders(mapOf("Authorization" to "Bearer $token"))
//          } else {
//            dataSpec
//          }
//        },
//      )
  }
}
