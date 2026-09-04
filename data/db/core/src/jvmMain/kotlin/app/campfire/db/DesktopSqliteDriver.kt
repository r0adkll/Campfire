// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Open (or create) a file-backed SQLite database for the desktop app and bring it up to
 * [schema]'s version, tracking the applied version in SQLite's `user_version` pragma the same
 * way the Android and native drivers do.
 *
 * - A brand new file gets [SqlSchema.create] and is stamped with [SqlSchema.version].
 * - A file stamped with an older version gets [SqlSchema.migrate] from that version.
 * - A file that already has tables but was never stamped (created by older desktop builds that
 *   ran `create` on every launch) is handed to [onLegacyDatabase], which must leave the file at
 *   a known version, either by rebuilding it or by stamping it once it matches the schema.
 */
fun desktopSqliteDriver(
  databaseFile: File,
  schema: SqlSchema<QueryResult.Value<Unit>>,
  onLegacyDatabase: SqlDriver.() -> Unit,
): SqlDriver {
  val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
  driver.upgradeTo(schema, onLegacyDatabase)
  return driver
}

internal fun SqlDriver.upgradeTo(
  schema: SqlSchema<QueryResult.Value<Unit>>,
  onLegacyDatabase: SqlDriver.() -> Unit,
) {
  if (userVersion() == 0L && hasUserTables()) {
    onLegacyDatabase()
  }

  val version = userVersion()
  when {
    version == 0L -> {
      schema.create(this).value
      setUserVersion(schema.version)
    }
    version < schema.version -> {
      schema.migrate(this, version, schema.version).value
      setUserVersion(schema.version)
    }
  }
}

/** Rebuild the database from scratch, dropping every user table before creating [schema]. */
fun SqlDriver.rebuild(schema: SqlSchema<QueryResult.Value<Unit>>) {
  DestructiveMigrationSchema.perform(this)
  schema.create(this).value
  setUserVersion(schema.version)
}

fun SqlDriver.userVersion(): Long = executeQuery(
  identifier = null,
  sql = "PRAGMA user_version",
  parameters = 0,
  mapper = { cursor ->
    cursor.next().value
    QueryResult.Value(cursor.getLong(0) ?: 0L)
  },
).value

fun SqlDriver.setUserVersion(version: Long) {
  execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}

fun SqlDriver.hasUserTables(): Boolean = executeQuery(
  identifier = null,
  sql = "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
  parameters = 0,
  mapper = { cursor ->
    cursor.next().value
    QueryResult.Value((cursor.getLong(0) ?: 0L) > 0)
  },
).value

fun SqlDriver.hasColumn(table: String, column: String): Boolean = executeQuery(
  identifier = null,
  sql = "PRAGMA table_info($table)",
  parameters = 0,
  mapper = { cursor ->
    var found = false
    while (cursor.next().value) {
      if (cursor.getString(1) == column) found = true
    }
    QueryResult.Value(found)
  },
).value
