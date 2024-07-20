package app.campfire.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.campfire.CampfireDatabase
import app.campfire.core.di.AppScope
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @AppScope
  @Provides
  fun provideAndroidSqlDriver(
    application: Application,
  ): SqlDriver = AndroidSqliteDriver(
    schema = CampfireDatabase.Schema,
    context = application,
    name = "campfire.db",
    callback = object : AndroidSqliteDriver.Callback(CampfireDatabase.Schema) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
      }
    },
  )
}
