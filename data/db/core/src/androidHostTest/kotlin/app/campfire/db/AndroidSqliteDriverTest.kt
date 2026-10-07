// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import assertk.assertThat
import assertk.assertions.containsExactly
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Opens the database through the driver the Android app uses. Its migrations have to run without
 * foreign key enforcement: a migration that rebuilds a table drops the old one, and with
 * enforcement on, that drop cascades through every table that references it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class AndroidSqliteDriverTest {

  private val application: Application = RuntimeEnvironment.getApplication()
  private val driver = object : SqlDelightDatabasePlatformComponent {}.provideAndroidSqlDriver(application)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun `upgrading keeps the signed-in account and its data`() {
    installBaseline {
      execSQL(
        """
        INSERT INTO server (url, userId, name, tent, scannerCoverProvider, metadataFileFormat,
          backupSchedule, sortingPrefixes, dateFormat, timeFormat, language, version)
        VALUES ('$SERVER', 'user-1', 'Home', 'red', 'google', 'json', '', '', 'MM/dd/yyyy', 'HH:mm',
          'en-us', '2.36.0')
        """,
      )
      execSQL(
        """
        INSERT INTO user (id, name, type, seriesHideFromContinueListening, createdAt,
          librariesAccessible, itemTagsAccessible, selectedLibraryId, serverUrl)
        VALUES ('user-1', 'user-1', 'user', '', 0, '', '', 'library', '$SERVER')
        """,
      )
      execSQL(
        """
        INSERT INTO bookmarks (userId, libraryItemId, title, timeInSeconds, createdAt)
        VALUES ('user-1', 'item-1', 'Bookmark', 42, '2026-10-06T00:00')
        """,
      )
      execSQL(
        """
        INSERT INTO session (id, userId, libraryItemId, playMethod, mediaPlayer, startedAt, updatedAt)
        VALUES ('session-1', 'user-1', 'item-1', 'DirectPlay', 'campfire', '2026-10-06T00:00',
          '2026-10-06T00:00')
        """,
      )
    }

    assertThat(strings("SELECT userId FROM server")).containsExactly("user-1")
    assertThat(strings("SELECT id FROM user")).containsExactly("user-1")
    assertThat(strings("SELECT userId FROM bookmarks")).containsExactly("user-1")
    assertThat(strings("SELECT id FROM session")).containsExactly("session-1")
  }

  @Test
  fun `foreign keys are enforced once the database is open`() {
    assertThat(longs("PRAGMA foreign_keys")).containsExactly(1L)
  }

  /**
   * Puts the version 1 baseline where the driver opens its database, so opening it runs every
   * migration.
   */
  private fun installBaseline(seed: SQLiteDatabase.() -> Unit) {
    val file = application.getDatabasePath(DATABASE_NAME)
    file.parentFile!!.mkdirs()
    File("src/commonMain/sqldelight/app/campfire/databases/1.db").copyTo(file, overwrite = true)
    SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
      db.seed()
      db.version = 1
    }
  }

  private fun strings(sql: String): List<String> = query(sql) { getString(0)!! }

  private fun longs(sql: String): List<Long> = query(sql) { getLong(0)!! }

  private fun <T : Any> query(sql: String, read: SqlCursor.() -> T): List<T> = driver.executeQuery(
    identifier = null,
    sql = sql,
    mapper = { cursor ->
      QueryResult.Value(buildList { while (cursor.next().value) add(cursor.read()) })
    },
    parameters = 0,
  ).value

  private companion object {
    const val DATABASE_NAME = "campfire.db"
    const val SERVER = "https://abs.example.com"
  }
}
