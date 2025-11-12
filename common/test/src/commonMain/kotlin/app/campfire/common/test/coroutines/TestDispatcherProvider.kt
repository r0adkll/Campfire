@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.common.test.coroutines

import app.campfire.core.coroutines.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

class TestDispatcherProvider(
  dispatcher: TestDispatcher = StandardTestDispatcher(),
) : DispatcherProvider(
  io = dispatcher,
  databaseRead = dispatcher,
  databaseWrite = dispatcher,
  main = dispatcher,
  computation = dispatcher,
)
