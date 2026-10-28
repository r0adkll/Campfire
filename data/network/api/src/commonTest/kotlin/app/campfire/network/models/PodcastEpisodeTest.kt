// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlin.test.Test

/**
 * When the server rescans an episode's changed audio file, it copies the scan's unset `index`
 * over the stored one, so the episode's `audioFile.index` comes back null from then on. One such
 * episode used to fail every response it appeared in.
 */
class PodcastEpisodeTest {

  @Test
  fun rescannedEpisode_decodes() {
    val episode = TestJson.decodeFromString<PodcastEpisode>(
      episodeJson(id = "ep_new", audioFile = RESCANNED_AUDIO_FILE),
    )

    assertThat(episode.audioFile.index).isNull()
  }

  @Test
  fun podcastItem_withARescannedEpisode_decodes() {
    val item = TestJson.decodeFromString<LibraryItemExpanded>(
      podcastItemJson(
        episodes = listOf(
          episodeJson(id = "ep_old", audioFile = AUDIO_FILE),
          episodeJson(id = "ep_new", audioFile = RESCANNED_AUDIO_FILE),
        ),
      ),
    )

    assertThat(item).isInstanceOf<LibraryItemExpanded.Podcast>()
      .prop(LibraryItemExpanded.Podcast::media)
      .prop(Podcast::episodes)
      .isNotNull()
      .hasSize(2)
  }

  @Test
  fun episodeShelf_withARescannedEpisode_decodes() {
    val json = """
      [
        {
          "id": "newest-episodes",
          "label": "Newest Episodes",
          "labelStringKey": "LabelNewestEpisodes",
          "type": "episode",
          "total": 1,
          "entities": [
            ${podcastItemJson(recentEpisode = episodeJson(id = "ep_new", audioFile = RESCANNED_AUDIO_FILE))}
          ]
        }
      ]
    """.trimIndent()

    val shelves = TestJson.decodeFromString<List<Shelf>>(json)

    assertThat(shelves.single()).isInstanceOf<Shelf.EpisodeShelf>()
      .prop(Shelf.EpisodeShelf::entities)
      .hasSize(1)
  }

  @Test
  fun recentEpisodesPage_withARescannedEpisode_decodes() {
    val episode = episodeJson(
      id = "ep_new",
      audioFile = RESCANNED_AUDIO_FILE,
      extras = """, "podcast": { "id": "pod_1", "title": "Some Show" }, "libraryId": "lib_1"""",
    )
    val json = """{ "episodes": [$episode], "limit": 50, "page": 0 }"""

    val page = TestJson.decodeFromString<PagedRecentEpisodesResponse>(json)

    assertThat(page.episodes).hasSize(1)
  }

  // PodcastEpisode.toOldJSONExpanded(), the shape every endpoint here serves.
  private fun episodeJson(id: String, audioFile: String, extras: String = "") = """
    {
      "libraryItemId": "li_1",
      "podcastId": "pod_1",
      "id": "$id",
      "oldEpisodeId": null,
      "index": null,
      "season": "",
      "episode": "",
      "episodeType": "full",
      "title": "Episode $id",
      "subtitle": "",
      "description": "<p>An episode</p>",
      "enclosure": { "url": "https://example.com/$id.mp3", "type": "audio/mpeg", "length": "1000" },
      "guid": "$id",
      "pubDate": "Mon, 26 Oct 2026 10:00:00 GMT",
      "chapters": [],
      "audioFile": $audioFile,
      "publishedAt": 1793008800000,
      "addedAt": 1793008800000,
      "updatedAt": 1793008800000,
      "audioTrack": ${audioTrackJson(id)},
      "size": 1000,
      "duration": 1800.5$extras
    }
  """.trimIndent()

  private fun audioTrackJson(id: String) = AUDIO_FILE.replaceFirst(
    "{",
    """{ "startOffset": 0, "title": "$id.mp3", "contentUrl": "/api/items/li_1/file/1234",""",
  )

  // LibraryItem.toOldJSONExpanded() with the minified fields it now spreads in, or the minified
  // shape carrying a shelf's recentEpisode.
  private fun podcastItemJson(
    episodes: List<String> = emptyList(),
    recentEpisode: String? = null,
  ) = """
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
        "libraryItemId": "li_1",
        "metadata": { "title": "Some Show", "author": "Some Host", "titleIgnorePrefix": "Some Show" },
        "coverPath": null,
        "tags": [],
        "numEpisodes": ${episodes.size},
        "autoDownloadEpisodes": false,
        "autoDownloadSchedule": "0 * * * *",
        "lastEpisodeCheck": 1733000000000,
        "maxEpisodesToKeep": 0,
        "maxNewEpisodesToDownload": 3,
        "size": 2000,
        "episodes": [${episodes.joinToString()}]
      },
      "numFiles": 3,
      "size": 2000,
      "libraryFiles": [],
      "recentEpisode": ${recentEpisode ?: "null"}
    }
  """.trimIndent()

  private companion object {
    // AudioFile.toJSON() for an episode as the server downloaded it.
    val AUDIO_FILE = audioFileJson(index = "1")

    // The same file after the server rescanned it.
    val RESCANNED_AUDIO_FILE = audioFileJson(index = "null")

    fun audioFileJson(index: String) = """
      {
        "index": $index,
        "ino": "1234",
        "metadata": {
          "filename": "episode.mp3",
          "ext": ".mp3",
          "path": "/podcasts/Some Show/episode.mp3",
          "relPath": "episode.mp3",
          "size": 1000,
          "mtimeMs": 1793008800000,
          "ctimeMs": 1793008800000,
          "birthtimeMs": 0
        },
        "addedAt": 1793008800000,
        "updatedAt": 1793008800000,
        "trackNumFromMeta": null,
        "discNumFromMeta": null,
        "trackNumFromFilename": null,
        "discNumFromFilename": null,
        "manuallyVerified": false,
        "exclude": false,
        "error": null,
        "format": "MP2/3 (MPEG audio layer 2/3)",
        "duration": 1800.5,
        "bitRate": 128000,
        "language": null,
        "codec": "mp3",
        "timeBase": "1/14112000",
        "channels": 2,
        "channelLayout": "stereo",
        "chapters": [],
        "embeddedCoverArt": null,
        "metaTags": { "tagTitle": "Episode", "tagEncoder": "Lavf59.27.100" },
        "mimeType": "audio/mpeg"
      }
    """.trimIndent()
  }
}
