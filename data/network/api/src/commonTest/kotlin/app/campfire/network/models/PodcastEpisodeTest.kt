// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlin.test.Test

class PodcastEpisodeTest {

  @Test
  fun audioFile_withoutProbeDetails_decodes() {
    // The server stores null for anything ffprobe didn't report.
    val audioFile = TestJson.decodeFromString<AudioFile>(UNPROBED_AUDIO_FILE)

    assertThat(audioFile.duration).isNull()
    assertThat(audioFile.bitRate).isNull()
    assertThat(audioFile.codec).isNull()
    assertThat(audioFile.timeBase).isNull()
    assertThat(audioFile.channels).isNull()
    assertThat(audioFile.channelLayout).isNull()
  }

  @Test
  fun audioTrack_withoutProbeDetails_decodes() {
    // An episode's track is a copy of its audio file, so it carries the same nulls.
    val track = TestJson.decodeFromString<AudioTrack>(UNPROBED_AUDIO_TRACK)

    assertThat(track.duration).isNull()
    assertThat(track.codec).isNull()
  }

  @Test
  fun enclosure_withNonNumericLength_decodes() {
    // The server passes the feed's `length` attribute through as text.
    val episode = TestJson.decodeFromString<PodcastEpisode>(episodeJson(enclosureLength = "unknown"))

    assertThat(episode.enclosure).isNotNull().prop(PodcastEpisode.Enclosure::length).isEqualTo("unknown")
  }

  @Test
  fun podcast_withFractionalEpisodeLimits_decodes() {
    // The server saves any number an admin enters for these limits.
    val podcast = TestJson.decodeFromString<Podcast>(
      """{"id": "pod_1", "maxEpisodesToKeep": 2.5, "maxNewEpisodesToDownload": 3}""",
    )

    assertThat(podcast.maxEpisodesToKeep).isEqualTo(2.5)
    assertThat(podcast.maxNewEpisodesToDownload).isEqualTo(3.0)
  }

  @Test
  fun recentEpisodesPage_withAnUnprobedEpisode_decodes() {
    // One such episode used to fail the whole page.
    val json = """
      {
        "episodes": [
          {
            "libraryItemId": "li_1",
            "podcastId": "pod_1",
            "id": "ep_1",
            "title": "Episode 1",
            "enclosure": { "url": "https://example.com/ep1.mp3", "type": "audio/mpeg", "length": "unknown" },
            "chapters": [],
            "audioFile": $UNPROBED_AUDIO_FILE,
            "audioTrack": $UNPROBED_AUDIO_TRACK,
            "addedAt": 1733000000000,
            "updatedAt": 1733000000000,
            "duration": 0,
            "size": 1000,
            "podcast": { "id": "pod_1", "maxEpisodesToKeep": 2.5, "episodes": [] },
            "libraryId": "lib_1"
          }
        ],
        "limit": "50",
        "page": "0"
      }
    """.trimIndent()

    val page = TestJson.decodeFromString<PagedRecentEpisodesResponse>(json)

    assertThat(page.episodes).hasSize(1)
  }

  private fun episodeJson(enclosureLength: String) = """
    {
      "libraryItemId": "li_1",
      "podcastId": "pod_1",
      "id": "ep_1",
      "title": "Episode 1",
      "enclosure": { "url": "https://example.com/ep1.mp3", "type": "audio/mpeg", "length": "$enclosureLength" },
      "chapters": [],
      "audioFile": $UNPROBED_AUDIO_FILE,
      "addedAt": 1733000000000,
      "updatedAt": 1733000000000
    }
  """.trimIndent()

  private companion object {
    // AudioFile.toJSON() for a file ffprobe reported no stream details for.
    val UNPROBED_AUDIO_FILE = """
      {
        "index": 1,
        "ino": "1234",
        "metadata": {
          "filename": "ep1.mp3",
          "ext": ".mp3",
          "path": "/podcasts/Some Show/ep1.mp3",
          "relPath": "ep1.mp3",
          "size": 1000,
          "mtimeMs": 1733000000000,
          "ctimeMs": 1733000000000,
          "birthtimeMs": 0
        },
        "addedAt": 1733000000000,
        "updatedAt": 1733000000000,
        "trackNumFromMeta": null,
        "discNumFromMeta": null,
        "trackNumFromFilename": null,
        "discNumFromFilename": null,
        "manuallyVerified": false,
        "exclude": false,
        "error": null,
        "format": "MP2/3 (MPEG audio layer 2/3)",
        "duration": null,
        "bitRate": null,
        "language": null,
        "codec": null,
        "timeBase": null,
        "channels": null,
        "channelLayout": null,
        "chapters": [],
        "embeddedCoverArt": null,
        "metaTags": {},
        "mimeType": "audio/mpeg"
      }
    """.trimIndent()

    val UNPROBED_AUDIO_TRACK = """
      {
        "index": 1,
        "startOffset": 0,
        "duration": null,
        "title": "ep1.mp3",
        "contentUrl": "/api/items/li_1/file/1234",
        "mimeType": "audio/mpeg",
        "codec": null,
        "metadata": {
          "filename": "ep1.mp3",
          "ext": ".mp3",
          "path": "/podcasts/Some Show/ep1.mp3",
          "relPath": "ep1.mp3",
          "size": 1000,
          "mtimeMs": 1733000000000,
          "ctimeMs": 1733000000000,
          "birthtimeMs": 0
        },
        "metaTags": {}
      }
    """.trimIndent()
  }
}
