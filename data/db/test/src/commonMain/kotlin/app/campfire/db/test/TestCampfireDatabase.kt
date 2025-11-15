package app.campfire.db.test

import app.campfire.CampfireDatabase
import app.campfire.db.DatabaseFactory
import app.cash.burst.coroutines.CoroutineTestFunction
import app.cash.burst.coroutines.CoroutineTestInterceptor
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest

/**
 * Init driver for each platform. Should *always* be called to setup test
 */
expect suspend fun createDriver(): SqlDriver

fun testDb(block: suspend CoroutineScope.(CampfireDatabase) -> Unit) = runTest {
  val driver = createDriver()
  val database = DatabaseFactory(driver).build()
  block(database)
  driver.close()
}

/**
 * A Burst interceptor for creating and closing in-memory test database for testing
 */
class CampfireDatabaseTestInterceptor : CoroutineTestInterceptor {
  private var _db: CampfireDatabase? = null
  val db: CampfireDatabase get() = _db!!

  override suspend fun intercept(testFunction: CoroutineTestFunction) {
    val driver = createDriver()
    _db = DatabaseFactory(driver).build()
    testFunction()
    driver.close()
    _db = null
  }
}
