package app.campfire.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @Provides
  fun provideAndroidSqlDriver(
    application: Application,
  ): SqlDriver = AndroidSqliteDriver(
    schema = CampfireDatabase.Schema.synchronous(),
    context = application,
    name = "campfire.db",
    callback = object : AndroidSqliteDriver.Callback(CampfireDatabase.Schema.synchronous()) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
      }
    },
  )
}
