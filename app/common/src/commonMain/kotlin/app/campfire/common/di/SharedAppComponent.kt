package app.campfire.common.di

import app.campfire.common.initializer.StartupInitializer
import app.campfire.core.app.ApplicationUrls
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

interface SharedAppComponent : CoreComponent

interface CoreComponent {

  val startupInitializer: StartupInitializer

  @SingleIn(AppScope::class)
  @Provides
  fun provideCoroutineDispatchers(): DispatcherProvider =
    DispatcherProvider(
      io = Dispatchers.IO,
      databaseWrite = Dispatchers.IO.limitedParallelism(1),
      databaseRead = Dispatchers.IO.limitedParallelism(4),
      computation = Dispatchers.Default,
      main = Dispatchers.Main,
    )

  @ForScope(AppScope::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideApplicationCoroutineScope(
    dispatcherProvider: DispatcherProvider,
  ): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @SingleIn(AppScope::class)
  @Provides
  fun provideFatherTime(): FatherTime = GrandFatherTime

  @Provides
  fun provideApplicationUrls(): ApplicationUrls = ApplicationUrls()
}
