package app.campfire.db

import app.campfire.CampfireDatabase
import app.cash.sqldelight.db.SqlDriver
import me.tatarka.inject.annotations.Inject

@Inject
class DatabaseFactory(
  private val driver: SqlDriver,
) {
  fun build(): CampfireDatabase = CampfireDatabase(
    driver = driver,
  )
}
