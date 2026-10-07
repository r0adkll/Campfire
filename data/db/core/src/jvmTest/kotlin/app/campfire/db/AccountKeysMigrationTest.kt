// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.CampfireDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * Migration 11 keys the server table by account. These run it the way the apps do, with foreign
 * keys off, on a database brought up to version 11 from the baseline, then switch enforcement on
 * the way Android does once the database is open.
 */
class AccountKeysMigrationTest {

  private val file = File.createTempFile("campfire-migration", ".db").apply {
    File("src/commonMain/sqldelight/app/campfire/databases/1.db").copyTo(this, overwrite = true)
  }
  private var driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
  private val schema = CampfireDatabase.Schema.synchronous()

  @AfterTest
  fun tearDown() {
    driver.close()
    file.delete()
  }

  @Test
  fun `accounts and their data survive the migration`() {
    schema.migrate(driver, 1, VERSION_BEFORE).value
    account("user-1", SERVER_A)
    bookmark("user-1")
    libraryItem("item-1", SERVER_A)

    migrateAndEnforce()

    assertThat(strings("SELECT userId FROM server")).containsExactly("user-1")
    assertThat(strings("SELECT id FROM user")).containsExactly("user-1")
    assertThat(strings("SELECT userId FROM bookmarks")).containsExactly("user-1")
    assertThat(strings("SELECT id FROM libraryItem")).containsExactly("item-1")
    assertThat(strings("SELECT libraryItemId FROM media")).containsExactly("item-1")
    assertThat(strings("SELECT \"table\" FROM pragma_foreign_key_check")).isEmpty()
  }

  @Test
  fun `server and user rows that don't pair up are dropped`() {
    schema.migrate(driver, 1, VERSION_BEFORE).value
    account("user-1", SERVER_A)
    exec(serverInsert("ghost", SERVER_B))
    exec(userInsert("orphan", SERVER_C))

    migrateAndEnforce()

    assertThat(strings("SELECT userId FROM server")).containsExactly("user-1")
    assertThat(strings("SELECT id FROM user")).containsExactly("user-1")
    assertThat(strings("SELECT \"table\" FROM pragma_foreign_key_check")).isEmpty()
  }

  @Test
  fun `several accounts share a server and its library items`() {
    schema.migrate(driver, 1, VERSION_BEFORE).value
    account("user-1", SERVER_A)
    bookmark("user-1")
    libraryItem("item-1", SERVER_A)
    migrateAndEnforce()

    account("user-2", SERVER_A)
    bookmark("user-2")
    assertThat(strings("SELECT userId FROM server ORDER BY userId")).containsExactly("user-1", "user-2")

    exec("DELETE FROM server WHERE userId = 'user-1'")
    assertThat(strings("SELECT id FROM user")).containsExactly("user-2")
    assertThat(strings("SELECT userId FROM bookmarks")).containsExactly("user-2")
    assertThat(strings("SELECT id FROM libraryItem")).containsExactly("item-1")

    exec("DELETE FROM server WHERE userId = 'user-2'")
    assertThat(strings("SELECT id FROM user")).isEmpty()
    assertThat(strings("SELECT id FROM libraryItem")).isEmpty()
    assertThat(strings("SELECT libraryItemId FROM media")).isEmpty()
  }

  @Test
  fun `removing an account keeps the library items of another server`() {
    schema.migrate(driver, 1, VERSION_BEFORE).value
    account("user-1", SERVER_A)
    account("user-2", SERVER_B)
    libraryItem("item-a", SERVER_A)
    libraryItem("item-b", SERVER_B)
    migrateAndEnforce()

    exec("DELETE FROM server WHERE userId = 'user-1'")

    assertThat(strings("SELECT id FROM libraryItem")).containsExactly("item-b")
  }

  private fun migrateAndEnforce() {
    schema.migrate(driver, VERSION_BEFORE, schema.version).value
    // The file-backed driver opens a connection per statement outside a transaction, so a PRAGMA
    // wouldn't stick: reopen with enforcement as a connection property instead.
    driver.close()
    driver = JdbcSqliteDriver(
      url = "jdbc:sqlite:${file.absolutePath}",
      properties = Properties().apply { put("foreign_keys", "true") },
    )
  }

  private fun account(userId: String, url: String) {
    exec(serverInsert(userId, url))
    exec(userInsert(userId, url))
  }

  private fun serverInsert(userId: String, url: String) =
    """
    INSERT INTO server (url, userId, name, scannerCoverProvider, metadataFileFormat, backupSchedule,
      sortingPrefixes, dateFormat, timeFormat, language, version)
    VALUES ('$url', '$userId', 'Home', 'google', 'json', '', '', 'MM/dd/yyyy', 'HH:mm', 'en-us', '2.36.0')
    """

  private fun userInsert(userId: String, url: String) =
    """
    INSERT INTO user (id, name, type, seriesHideFromContinueListening, createdAt, librariesAccessible,
      itemTagsAccessible, selectedLibraryId, serverUrl)
    VALUES ('$userId', '$userId', 'user', '', 0, '', '', 'library', '$url')
    """

  private fun bookmark(userId: String) {
    exec(
      """
      INSERT INTO bookmarks (userId, libraryItemId, title, timeInSeconds, createdAt)
      VALUES ('$userId', 'item-1', 'Bookmark', 42, '2026-10-06T00:00')
      """,
    )
  }

  private fun libraryItem(id: String, url: String) {
    exec(
      """
      INSERT INTO libraryItem (id, ino, libraryId, folderId, path, relPath, mtimeMs, ctimeMs,
        birthtimeMs, addedAt, updatedAt, mediaType, numFiles, size, serverUrl)
      VALUES ('$id', 'ino', 'library', 'folder', 'path', 'relPath', 0, 0, 0, 0, 0, 'Book', 1, 0, '$url')
      """,
    )
    exec(
      """
      INSERT INTO media (mediaId, numTracks, numAudioFiles, numChapters, numMissingParts,
        numInvalidAudioFiles, durationInMillis, sizeInBytes, libraryItemId)
      VALUES ('media-$id', 1, 1, 1, 0, 0, 0, 0, '$id')
      """,
    )
  }

  private fun exec(sql: String) {
    driver.execute(null, sql.trimIndent(), 0)
  }

  private fun strings(sql: String): List<String> = driver.executeQuery(
    identifier = null,
    sql = sql,
    mapper = { cursor ->
      QueryResult.Value(buildList { while (cursor.next().value) add(cursor.getString(0)!!) })
    },
    parameters = 0,
  ).value

  private companion object {
    // The last version that keyed the server table by URL
    const val VERSION_BEFORE = 11L

    const val SERVER_A = "https://abs.example.com"
    const val SERVER_B = "http://10.0.2.2:13379"
    const val SERVER_C = "http://127.0.0.1:13379"
  }
}
