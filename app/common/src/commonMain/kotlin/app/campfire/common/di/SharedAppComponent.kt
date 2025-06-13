package app.campfire.common.di

import app.campfire.common.initializer.StartupInitializer
import app.campfire.core.app.ApplicationUrls
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForAppScope
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

interface SharedAppComponent {

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

  // FIXME: https://github.com/ZacSweers/metro/issues/444
  //   Workaround in metro gradle config
  // FIXME: https://github.com/ZacSweers/metro/pull/407
  //   Fixed in Kotlin 2.2.0 + Future Metro version
  //   Should probably re-think about how we approach DI scoped primitives like CoroutineScope and the like
  @SingleIn(AppScope::class)
  @Provides
  @ForAppScope
  fun provideApplicationCoroutineScope(
    dispatcherProvider: DispatcherProvider,
  ): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  @SingleIn(AppScope::class)
  @Provides
  fun provideFatherTime(): FatherTime = GrandFatherTime

  @Provides
  fun provideApplicationUrls(): ApplicationUrls = ApplicationUrls()
}
