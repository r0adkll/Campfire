// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.CampfireDatabase
import app.campfire.data.Collections
import app.campfire.data.CollectionsBookJoin
import app.campfire.data.Library
import app.campfire.data.Series
import app.campfire.data.SeriesBookJoin
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

/**
 * Accounts on the same server see the same library, series and collection ids, so each account
 * keeps its own copy of those rows. Foreign keys are enforced, as on Android.
 */
class PerAccountCachesTest {

  private val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
    CampfireDatabase.Schema.synchronous().create(it)
    it.execute(null, "PRAGMA foreign_keys = ON", 0)
  }
  private val db = DatabaseFactory(driver).build()

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun `each account keeps its own copy of a library`() = runBlocking {
    accounts()
    db.librariesQueries.insert(library(USER_1))
    db.librariesQueries.insert(library(USER_2))

    db.librariesQueries.deleteById(LIBRARY_ID, USER_2)

    assertThat(db.librariesQueries.selectAll(USER_1).executeAsList().map { it.userId }).containsExactly(USER_1)
    assertThat(db.librariesQueries.selectAll(USER_2).executeAsList()).isEmpty()
  }

  @Test
  fun `removing an account keeps the other account's series and collections`() = runBlocking {
    accounts()
    for (userId in listOf(USER_1, USER_2)) {
      db.seriesQueries.insertOrIgnore(series(userId))
      db.seriesBookJoinQueries.insert(SeriesBookJoin(SERIES_ID, ITEM_ID, userId))
      db.collectionsQueries.insert(collection(userId))
      db.collectionsBookJoinQueries.insert(CollectionsBookJoin(COLLECTION_ID, ITEM_ID, 0, userId))
    }

    exec("DELETE FROM server WHERE userId = '$USER_1'")

    assertThat(db.seriesQueries.selectByLibraryId(LIBRARY_ID, USER_2).executeAsList().map { it.id })
      .containsExactly(SERIES_ID)
    assertThat(strings("SELECT userId FROM seriesBookJoin")).containsExactly(USER_2)
    assertThat(db.collectionsQueries.selectAll(USER_2).executeAsList().map { it.id }).containsExactly(COLLECTION_ID)
    assertThat(strings("SELECT userId FROM collectionsBookJoin")).containsExactly(USER_2)
    assertThat(strings("SELECT userId FROM series")).containsExactly(USER_2)
    assertThat(strings("SELECT userId FROM collections")).containsExactly(USER_2)
  }

  @Test
  fun `syncing one account's collections leaves the other account's alone`() = runBlocking {
    accounts()
    db.collectionsQueries.insert(collection(USER_1))
    db.collectionsQueries.insert(collection(USER_2))

    db.collectionsQueries.deleteOld(USER_2, listOf("another-collection"))

    assertThat(db.collectionsQueries.selectAll(USER_1).executeAsList().map { it.id }).containsExactly(COLLECTION_ID)
    assertThat(db.collectionsQueries.selectAll(USER_2).executeAsList()).isEmpty()
  }

  @Test
  fun `shelves, search results and pages read the series copy of their own account`() = runBlocking {
    accounts()
    db.seriesQueries.insertOrIgnore(series(USER_1))
    db.seriesQueries.insertOrIgnore(series(USER_2, name = "Seen by bob"))
    exec(
      """
      INSERT INTO shelf (id, libraryId, label, labelStringKey, total, type, userId)
      VALUES ('shelf-2', '$LIBRARY_ID', 'Series', 'series', 1, 'SERIES', '$USER_2')
      """,
    )
    exec("INSERT INTO shelfJoin (shelfId, entityId) VALUES ('shelf-2', '$SERIES_ID')")
    exec("INSERT INTO search (key, userId) VALUES ('search-2', '$USER_2')")
    exec("INSERT INTO search_series (searchKey, seriesId) VALUES ('search-2', '$SERIES_ID')")
    exec(
      """
      INSERT INTO seriesPage (id, userId, libraryId, input, page, nextPage, total, updatedAt)
      VALUES (1, '$USER_2', '$LIBRARY_ID', 'all', 0, NULL, 1, 0)
      """,
    )
    exec("INSERT INTO seriesPageJoin (pageId, pageIndex, seriesId) VALUES (1, 0, '$SERIES_ID')")

    assertThat(db.seriesQueries.selectByShelfId("shelf-2").executeAsList().map { it.name })
      .containsExactly("Seen by bob")
    assertThat(db.searchQueries.searchSeries("search-2").executeAsList().map { it.name })
      .containsExactly("Seen by bob")
    assertThat(
      db.seriesPageQueries.selectSeriesWithLimitAndOffset(USER_2, LIBRARY_ID, "all", 10, 0)
        .executeAsList()
        .map { it.name },
    ).containsExactly("Seen by bob")
  }

  private fun accounts() {
    for (userId in listOf(USER_1, USER_2)) {
      exec(
        """
        INSERT INTO server (url, userId, name, scannerCoverProvider, metadataFileFormat, backupSchedule,
          sortingPrefixes, dateFormat, timeFormat, language, version)
        VALUES ('$SERVER_URL', '$userId', 'Home', 'google', 'json', '', '', 'MM/dd/yyyy', 'HH:mm', 'en-us', '2.36.0')
        """,
      )
      exec(
        """
        INSERT INTO user (id, name, type, seriesHideFromContinueListening, createdAt, librariesAccessible,
          itemTagsAccessible, selectedLibraryId, serverUrl)
        VALUES ('$userId', '$userId', 'user', '', 0, '', '', '$LIBRARY_ID', '$SERVER_URL')
        """,
      )
    }
    exec(
      """
      INSERT INTO libraryItem (id, ino, libraryId, folderId, path, relPath, mtimeMs, ctimeMs,
        birthtimeMs, addedAt, updatedAt, mediaType, numFiles, size, serverUrl)
      VALUES ('$ITEM_ID', 'ino', '$LIBRARY_ID', 'folder', 'path', 'relPath', 0, 0, 0, 0, 0, 'Book', 1, 0, '$SERVER_URL')
      """,
    )
  }

  private fun library(userId: String) = Library(
    id = LIBRARY_ID,
    name = "Audiobooks",
    displayOrder = 0,
    icon = "database",
    mediaType = "book",
    provider = "audible",
    createdAt = 0,
    lastUpdate = 0,
    coverAspectRatio = 1,
    audiobooksOnly = false,
    userId = userId,
  )

  private fun series(userId: String, name: String = "The Stormlight Archive") = Series(
    id = SERIES_ID,
    name = name,
    description = null,
    addedAt = 0,
    updatedAt = 0,
    inProgress = false,
    hasActiveBook = false,
    hideFromContinueListening = false,
    bookInProgressLastUpdate = null,
    firstBookUnreadId = null,
    libraryId = LIBRARY_ID,
    userId = userId,
  )

  private fun collection(userId: String) = Collections(
    id = COLLECTION_ID,
    name = "Favorites",
    description = null,
    cover = null,
    coverFullPath = null,
    updatedAt = 0,
    createdAt = 0,
    libraryId = LIBRARY_ID,
    userId = userId,
  )

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
    const val SERVER_URL = "http://10.0.2.2:13379"
    const val USER_1 = "demo"
    const val USER_2 = "bob"
    const val LIBRARY_ID = "library"
    const val SERIES_ID = "series"
    const val COLLECTION_ID = "collection"
    const val ITEM_ID = "item"
  }
}
