// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.CampfireDatabase
import app.campfire.core.model.MediaType
import app.campfire.data.LibraryItem
import app.campfire.data.PodcastEpisode
import app.campfire.data.PodcastMedia
import app.campfire.db.DatabaseFactory
import app.campfire.db.test.createDriver
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class UpsertsTest {

  @Test
  fun upsert_refreshesARowStoredFromPartialData() = upsertTest { db ->
    // What Latest Episodes stores for a podcast it hasn't seen in a list yet.
    db.libraryItemsQueries.insertOrIgnore(libraryItem(path = "", size = 5))
    db.podcastMediaQueries.insertOrIgnore(podcastMedia(numEpisodes = 0, sizeInBytes = 0))
    db.podcastEpisodeQueries.insert(episode)

    db.libraryItemsQueries.upsert(libraryItem(path = "/podcasts/Some Show", size = 123_456))
    db.podcastMediaQueries.upsert(podcastMedia(numEpisodes = 31, sizeInBytes = 123_456))

    val row = db.libraryItemsQueries.selectForPodcastId(ITEM_ID).awaitAsOne()
    assertThat(row.path).isEqualTo("/podcasts/Some Show")
    assertThat(row.size).isEqualTo(123_456)
    assertThat(row.numEpisodes).isEqualTo(31)
    assertThat(row.sizeInBytes).isEqualTo(123_456)
  }

  @Test
  fun upsert_keepsTheItemsEpisodes() = upsertTest { db ->
    db.libraryItemsQueries.insertOrIgnore(libraryItem(path = "", size = 5))
    db.podcastMediaQueries.insertOrIgnore(podcastMedia(numEpisodes = 0, sizeInBytes = 0))
    db.podcastEpisodeQueries.insert(episode)

    db.libraryItemsQueries.upsert(libraryItem(path = "/podcasts/Some Show", size = 123_456))
    db.podcastMediaQueries.upsert(podcastMedia(numEpisodes = 31, sizeInBytes = 123_456))

    // INSERT OR REPLACE would have deleted the episode through its foreign keys.
    assertThat(db.podcastEpisodeQueries.selectForLibraryItemId(ITEM_ID).awaitAsList()).hasSize(1)
  }

  @Test
  fun upsert_insertsAMissingRow() = upsertTest { db ->
    db.libraryItemsQueries.upsert(libraryItem(path = "/podcasts/Some Show", size = 123_456))
    db.podcastMediaQueries.upsert(podcastMedia(numEpisodes = 31, sizeInBytes = 123_456))

    val row = db.libraryItemsQueries.selectForPodcastId(ITEM_ID).awaitAsOne()
    assertThat(row.path).isEqualTo("/podcasts/Some Show")
    assertThat(row.numEpisodes).isEqualTo(31)
  }

  private fun upsertTest(block: suspend (CampfireDatabase) -> Unit) = runTest {
    val driver = createDriver()
    try {
      // Android enforces foreign keys, which is what makes REPLACE cascade.
      driver.execute(null, "PRAGMA foreign_keys = ON", 0)
      driver.execute(
        null,
        """
        INSERT INTO server (url, userId, name, scannerCoverProvider, metadataFileFormat,
          backupSchedule, sortingPrefixes, dateFormat, timeFormat, language, version)
        VALUES ('$SERVER_URL', 'user_1', 'Test', 'google', 'json', '', '', 'MM/dd/yyyy', 'HH:mm',
          'en-us', '2.37.1')
        """.trimIndent(),
        0,
      )
      block(DatabaseFactory(driver).build())
    } finally {
      driver.close()
    }
  }

  private fun libraryItem(path: String, size: Long) = LibraryItem(
    id = ITEM_ID,
    ino = "",
    libraryId = "lib_1",
    oldLibraryItemId = null,
    folderId = "",
    path = path,
    relPath = "",
    isFile = false,
    mtimeMs = 0,
    ctimeMs = 0,
    birthtimeMs = 0,
    addedAt = 0,
    updatedAt = 0,
    isMissing = false,
    isInvalid = false,
    mediaType = MediaType.Podcast,
    numFiles = 0,
    size = size,
    serverUrl = SERVER_URL,
  )

  private fun podcastMedia(numEpisodes: Int, sizeInBytes: Long) = PodcastMedia(
    mediaId = MEDIA_ID,
    libraryItemId = ITEM_ID,
    coverPath = null,
    tags = null,
    sizeInBytes = sizeInBytes,
    numEpisodes = numEpisodes,
    autoDownloadEpisodes = false,
    autoDownloadSchedule = null,
    lastEpisodeCheckMillis = null,
    maxEpisodesToKeep = 0,
    maxNewEpisodesToDownload = 3,
    latestEpisodePublishedAtMillis = null,
    metadata_title = "Some Show",
    metadata_titleIgnorePrefix = "Some Show",
    metadata_author = null,
    metadata_description = null,
    metadata_releaseDate = null,
    metadata_genres = null,
    metadata_feedUrl = null,
    metadata_imageUrl = null,
    metadata_itunesPageUrl = null,
    metadata_itunesId = null,
    metadata_itunesArtistId = null,
    metadata_explicit = false,
    metadata_language = null,
    metadata_podcastType = null,
  )

  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = ITEM_ID,
    podcastMediaId = MEDIA_ID,
    episodeIndex = null,
    season = null,
    episodeNumber = null,
    episodeType = null,
    title = "Episode 1",
    subtitle = null,
    description = null,
    pubDate = null,
    publishedAtMillis = null,
    addedAtMillis = 0,
    updatedAtMillis = 0,
    durationInMillis = 0,
    sizeInBytes = 0,
  )

  private companion object {
    const val SERVER_URL = "https://abs.example.com"
    const val ITEM_ID = "li_1"
    const val MEDIA_ID = "pod_1"
  }
}
