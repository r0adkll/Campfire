// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class PlaySessionTest {

  @Test
  fun directPlaySession_parsesFullTrackRecords() {
    // Direct-play responses carry one full audio track record per file, with static
    // authenticated file URLs. Unmodeled fields (libraryItem, mediaMetadata, deviceInfo, …)
    // must be ignored.
    val json = """
      {
        "id": "session-1",
        "userId": "user-1",
        "libraryId": "lib-1",
        "libraryItemId": "item-1",
        "episodeId": null,
        "mediaType": "book",
        "mediaMetadata": { "title": "A Book" },
        "chapters": [],
        "displayTitle": "A Book",
        "duration": 3600.5,
        "playMethod": 0,
        "mediaPlayer": "unknown",
        "currentTime": 120.25,
        "serverVersion": "2.34.0",
        "deviceInfo": { "id": "dev-1", "deviceId": "dev-1" },
        "audioTracks": [
          {
            "index": 1,
            "startOffset": 0,
            "duration": 1800.5,
            "title": "part1.m4b",
            "contentUrl": "/api/items/item-1/file/12345",
            "mimeType": "audio/mp4",
            "codec": "aac",
            "metadata": { "filename": "part1.m4b", "ext": ".m4b" }
          },
          {
            "index": 2,
            "startOffset": 1800.5,
            "duration": 1800,
            "title": "part2.m4b",
            "contentUrl": "/api/items/item-1/file/12346",
            "mimeType": "audio/mp4",
            "codec": "aac",
            "metadata": { "filename": "part2.m4b", "ext": ".m4b" }
          }
        ]
      }
    """.trimIndent()

    val session = TestJson.decodeFromString(PlaySession.serializer(), json)

    assertThat(session.id).isEqualTo("session-1")
    assertThat(session.playMethod).isEqualTo(0)
    assertThat(session.audioTracks).hasSize(2)
    assertThat(session.audioTracks[0].index).isEqualTo(1)
    assertThat(session.audioTracks[0].contentUrl).isEqualTo("/api/items/item-1/file/12345")
    assertThat(session.audioTracks[1].startOffset).isEqualTo(1800.5)
  }

  @Test
  fun transcodeSession_parsesThinTrackWithNullFields() {
    // Transcode responses carry a single thin track whose metadata and codec are null and
    // whose contentUrl is the per-session HLS playlist.
    val json = """
      {
        "id": "session-2",
        "libraryItemId": "item-1",
        "playMethod": 2,
        "duration": 3600,
        "audioTracks": [
          {
            "index": 1,
            "startOffset": 0,
            "duration": 3600,
            "title": "output.m3u8",
            "contentUrl": "/hls/session-2/output.m3u8",
            "mimeType": null,
            "codec": null,
            "metadata": null
          }
        ]
      }
    """.trimIndent()

    val session = TestJson.decodeFromString(PlaySession.serializer(), json)

    assertThat(session.playMethod).isEqualTo(2)
    assertThat(session.audioTracks).hasSize(1)
    assertThat(session.audioTracks[0].contentUrl).isEqualTo("/hls/session-2/output.m3u8")
    assertThat(session.audioTracks[0].mimeType).isNull()
    assertThat(session.audioTracks[0].codec).isNull()
    assertThat(session.episodeId).isNull()
  }

  @Test
  fun podcastSession_parsesEpisodeId() {
    val json = """
      {
        "id": "session-3",
        "libraryItemId": "item-2",
        "episodeId": "ep-9",
        "mediaType": "podcast",
        "playMethod": 0,
        "audioTracks": [
          {
            "index": 1,
            "startOffset": 0,
            "duration": 1234.5,
            "contentUrl": "/api/items/item-2/file/999",
            "mimeType": "audio/mpeg"
          }
        ]
      }
    """.trimIndent()

    val session = TestJson.decodeFromString(PlaySession.serializer(), json)

    assertThat(session.episodeId).isEqualTo("ep-9")
    assertThat(session.mediaType).isEqualTo("podcast")
  }
}
