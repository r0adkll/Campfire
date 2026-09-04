// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class DesktopSqliteDriverTest {

  @Test
  fun `fresh database is created and stamped with the schema version`() {
    val schema = RecordingSchema(version = 3)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    driver.upgradeTo(schema) { error("legacy hook should not run") }

    assertThat(schema.calls).containsExactly("create")
    assertThat(driver.userVersion()).isEqualTo(3L)
    assertThat(driver.hasColumn("thing", "id")).isTrue()
  }

  @Test
  fun `database already at the schema version is left alone`() {
    val schema = RecordingSchema(version = 3)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.upgradeTo(schema) { error("legacy hook should not run") }
    schema.calls.clear()

    driver.upgradeTo(schema) { error("legacy hook should not run") }

    assertThat(schema.calls).containsExactly()
    assertThat(driver.userVersion()).isEqualTo(3L)
  }

  @Test
  fun `database at an older version is migrated and restamped`() {
    val schema = RecordingSchema(version = 3)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE thing (id INTEGER PRIMARY KEY)", 0)
    driver.setUserVersion(1)

    driver.upgradeTo(schema) { error("legacy hook should not run") }

    assertThat(schema.calls).containsExactly("migrate 1 -> 3")
    assertThat(driver.userVersion()).isEqualTo(3L)
  }

  @Test
  fun `unstamped database with tables runs the legacy hook before upgrading`() {
    val schema = RecordingSchema(version = 3)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE stale (id INTEGER PRIMARY KEY)", 0)
    var legacyCalled = false

    driver.upgradeTo(schema) {
      legacyCalled = true
      rebuild(schema)
    }

    assertThat(legacyCalled).isTrue()
    assertThat(schema.calls).containsExactly("create")
    assertThat(driver.userVersion()).isEqualTo(3L)
    assertThat(driver.hasColumn("stale", "id")).isFalse()
    assertThat(driver.hasColumn("thing", "id")).isTrue()
  }

  @Test
  fun `legacy hook that stamps the version skips create and migrate`() {
    val schema = RecordingSchema(version = 3)
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE thing (id INTEGER PRIMARY KEY)", 0)

    driver.upgradeTo(schema) { setUserVersion(schema.version) }

    assertThat(schema.calls).containsExactly()
    assertThat(driver.userVersion()).isEqualTo(3L)
  }

  @Test
  fun `hasUserTables ignores sqlite internal tables`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    assertThat(driver.hasUserTables()).isFalse()

    driver.execute(null, "CREATE TABLE thing (id INTEGER PRIMARY KEY AUTOINCREMENT)", 0)
    driver.execute(null, "INSERT INTO thing DEFAULT VALUES", 0)

    assertThat(driver.hasUserTables()).isTrue()
  }

  private class RecordingSchema(override val version: Long) : SqlSchema<QueryResult.Value<Unit>> {
    val calls = mutableListOf<String>()

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      calls += "create"
      driver.execute(null, "CREATE TABLE thing (id INTEGER PRIMARY KEY)", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
      calls += "migrate $oldVersion -> $newVersion"
      return QueryResult.Unit
    }
  }
}
