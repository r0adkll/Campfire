package app.campfire.common.di

import app.campfire.account.api.UserSessionManager
import app.campfire.common.initializer.StartupInitializer
import app.campfire.core.app.ApplicationUrls
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import me.tatarka.inject.annotations.Provides

interface SharedAppComponent : CoreComponent

interface CoreComponent {

  val startupInitializer: StartupInitializer
  val sessionManager: UserSessionManager
  val userComponentManager: UserComponentManager

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
