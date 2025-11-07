package app.campfire.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

class DispatcherProvider(
  val io: CoroutineDispatcher,
  val databaseWrite: CoroutineDispatcher,
  val databaseRead: CoroutineDispatcher,
  val computation: CoroutineDispatcher,
  val main: CoroutineDispatcher,
)
