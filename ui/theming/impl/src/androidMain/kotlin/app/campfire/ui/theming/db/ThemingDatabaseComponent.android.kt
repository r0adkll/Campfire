package app.campfire.ui.theming.db

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.themes.CampfireThemeDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides

actual interface SqlDelightDatabasePlatformComponent {

  @SingleIn(AppScope::class)
  @ThemingDb
  @Provides
  fun provideThemingAndroidSqlDriver(
    application: Application,
  ): SqlDriver = AndroidSqliteDriver(
    schema = CampfireThemeDatabase.Schema.synchronous(),
    context = application,
    name = "campfire_themes.db",
    callback = object : AndroidSqliteDriver.Callback(CampfireThemeDatabase.Schema.synchronous()) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
      }
    },
  )
}
