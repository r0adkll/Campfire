// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.purge

import app.campfire.CampfireDatabase
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * An in-memory database seeded through raw SQL (only the NOT NULL columns matter here), with
 * foreign keys switched on afterwards the way Android runs — the test drivers leave them off,
 * which would hide the join tables' ON DELETE NO ACTION blocking a delete.
 */
class PurgeTestDatabase(private val driver: SqlDriver) {

  val db: CampfireDatabase = DatabaseFactory(driver).build()

  fun enforceForeignKeys() {
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
  }

  fun book(id: String, libraryId: String = LIBRARY_ID, authorName: String? = null) {
    exec(
      """
      INSERT INTO libraryItem (id, ino, libraryId, folderId, path, relPath, mtimeMs, ctimeMs,
        birthtimeMs, addedAt, updatedAt, mediaType, numFiles, size, serverUrl)
      VALUES ('$id', 'ino', '$libraryId', 'folder', 'path', 'relPath', 0, 0, 0, 0, 0, 'Book', 1, 0, 'server')
      """,
    )
    exec(
      """
      INSERT INTO media (mediaId, numTracks, numAudioFiles, numChapters, numMissingParts,
        numInvalidAudioFiles, durationInMillis, sizeInBytes, metadata_authorName, libraryItemId)
      VALUES ('media-$id', 1, 1, 1, 0, 0, 0, 0, ${authorName?.let { "'$it'" } ?: "NULL"}, '$id')
      """,
    )
    exec("INSERT INTO mediaChapters (mediaId, id, start, end, title) VALUES ('media-$id', 0, 0, 1, 'Chapter')")
  }

  fun linkEverywhere(itemId: String) {
    exec("INSERT INTO seriesBookJoin (seriesId, libraryItemId) VALUES ('series', '$itemId')")
    exec(
      "INSERT INTO collectionsBookJoin (collectionsId, libraryItemId, itemOrder) VALUES ('collection', '$itemId', 0)",
    )
    exec("INSERT INTO playlistItemJoin (playlistId, libraryItemId, itemOrder) VALUES ('playlist', '$itemId', 0)")
    exec("INSERT INTO libraryItemPageJoin (pageId, pageIndex, libraryItemId) VALUES (1, 0, '$itemId')")
    exec("INSERT INTO shelfJoin (shelfId, entityId) VALUES ('shelf', '$itemId')")
    exec("INSERT INTO search_books (searchKey, libraryItemId) VALUES ('search', '$itemId')")
  }

  fun ids(table: String, column: String = "libraryItemId"): List<String> {
    return driver.executeQuery(
      identifier = null,
      sql = "SELECT $column FROM $table ORDER BY $column",
      mapper = { cursor ->
        app.cash.sqldelight.db.QueryResult.Value(
          buildList { while (cursor.next().value) add(cursor.getString(0)!!) },
        )
      },
      parameters = 0,
    ).value
  }

  private fun exec(sql: String) {
    driver.execute(null, sql.trimIndent(), 0)
  }

  companion object {
    const val LIBRARY_ID = "library"
  }
}

fun purgeTest(block: suspend TestScope.(PurgeTestDatabase) -> Unit) = runTest {
  val driver = createDriver()
  try {
    block(PurgeTestDatabase(driver))
  } finally {
    driver.close()
  }
}
