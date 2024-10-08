package app.campfire.shared.di

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.time.FatherTime
import app.campfire.core.time.GrandFatherTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import me.tatarka.inject.annotations.Provides

interface SharedAppComponent : CoreComponent

interface CoreComponent {

  @OptIn(ExperimentalCoroutinesApi::class)
  @SingleIn(AppScope::class)
  @Provides
  fun provideCoroutineDispatchers(): app.campfire.core.coroutines.DispatcherProvider =
    app.campfire.core.coroutines.DispatcherProvider(
      io = Dispatchers.IO,
      databaseWrite = Dispatchers.IO.limitedParallelism(1),
      databaseRead = Dispatchers.IO.limitedParallelism(4),
      computation = Dispatchers.Default,
      main = Dispatchers.Main,
    )

  @SingleIn(AppScope::class)
  @Provides
  fun provideFatherTime(): FatherTime = GrandFatherTime
}
