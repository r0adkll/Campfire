// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

class PodcastMetadataTest {

  @Test
  fun blankReleaseDate_decodes() {
    // The server stores releaseDate as free text: podcasts added from an RSS URL in the web
    // client, or through an OPML import, are created with an empty string.
    val item = TestJson.decodeFromString<LibraryItemMinified>(podcastItemJson(releaseDate = "\"\""))

    assertThat(item).isInstanceOf<LibraryItemMinified.Podcast>()
      .prop(LibraryItemMinified.Podcast::media)
      .prop(Podcast::metadata)
      .isEqualTo(expectedMetadata(releaseDate = ""))
  }

  @Test
  fun nonIsoReleaseDate_decodesVerbatim() {
    // Users can type anything into the release date field.
    val item = TestJson.decodeFromString<LibraryItemMinified>(podcastItemJson(releaseDate = "\"2019\""))

    assertThat(item).isInstanceOf<LibraryItemMinified.Podcast>()
      .prop(LibraryItemMinified.Podcast::media)
      .prop(Podcast::metadata)
      .isEqualTo(expectedMetadata(releaseDate = "2019"))
  }

  @Test
  fun podcastShelf_withBlankReleaseDate_decodes() {
    // One podcast like this used to fail the whole personalized home response.
    val json = """
      [
        {
          "id": "recently-added",
          "label": "Recently Added",
          "labelStringKey": "LabelRecentlyAdded",
          "type": "podcast",
          "total": 1,
          "entities": [${podcastItemJson(releaseDate = "\"\"")}]
        }
      ]
    """.trimIndent()

    val shelves = TestJson.decodeFromString<List<Shelf>>(json)

    assertThat(shelves).hasSize(1)
    assertThat(shelves.single()).isInstanceOf<Shelf.PodcastShelf>()
      .prop(Shelf.PodcastShelf::entities)
      .hasSize(1)
  }

  private fun expectedMetadata(releaseDate: String) = PodcastMetadata(
    title = "Some Show",
    author = "Some Host",
    description = "A show about things",
    releaseDate = releaseDate,
    genres = listOf("News"),
    feedUrl = "https://example.com/feed.xml",
    imageUrl = "https://example.com/cover.jpg",
    itunesPageUrl = "",
    itunesId = "",
    itunesArtistId = "",
    explicit = false,
    language = "",
    type = null,
  )

  // The minified shape served by the library items and personalized shelf endpoints.
  private fun podcastItemJson(releaseDate: String) = """
    {
      "id": "li_1",
      "ino": "1234",
      "oldLibraryItemId": null,
      "libraryId": "lib_1",
      "folderId": "fol_1",
      "path": "/podcasts/Some Show",
      "relPath": "Some Show",
      "isFile": false,
      "mtimeMs": 1733000000000,
      "ctimeMs": 1733000000000,
      "birthtimeMs": 0,
      "addedAt": 1733000000000,
      "updatedAt": 1733000000000,
      "isMissing": false,
      "isInvalid": false,
      "mediaType": "podcast",
      "media": {
        "id": "pod_1",
        "metadata": {
          "title": "Some Show",
          "author": "Some Host",
          "description": "A show about things",
          "releaseDate": $releaseDate,
          "genres": ["News"],
          "feedUrl": "https://example.com/feed.xml",
          "imageUrl": "https://example.com/cover.jpg",
          "itunesPageUrl": "",
          "itunesId": "",
          "itunesArtistId": "",
          "explicit": false,
          "language": "",
          "type": null,
          "titleIgnorePrefix": "Some Show"
        },
        "coverPath": "/metadata/items/li_1/cover.jpg",
        "tags": [],
        "numEpisodes": 2,
        "autoDownloadEpisodes": false,
        "autoDownloadSchedule": "0 * * * *",
        "lastEpisodeCheck": 1733000000000,
        "maxEpisodesToKeep": 0,
        "maxNewEpisodesToDownload": 3,
        "size": 123456
      },
      "numFiles": 3,
      "size": 123456
    }
  """.trimIndent()
}
