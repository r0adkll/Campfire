package app.campfire.audioplayer.impl.di

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesTo
import java.io.File
import me.tatarka.inject.annotations.Provides

@ContributesTo(AppScope::class)
interface DatabaseProviderComponent {

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
}
