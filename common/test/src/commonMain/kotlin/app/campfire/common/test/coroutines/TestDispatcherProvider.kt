@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.common.test.coroutines

import app.campfire.core.coroutines.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

class TestDispatcherProvider(
  dispatcher: TestDispatcher = StandardTestDispatcher(),
) : DispatcherProvider(
  io = dispatcher,
  databaseRead = dispatcher,
  databaseWrite = dispatcher,
  main = dispatcher,
  computation = dispatcher,
)

fun TestScope.asTestDispatcherProvider(): DispatcherProvider {
  return TestDispatcherProvider(StandardTestDispatcher(testScheduler))
}
