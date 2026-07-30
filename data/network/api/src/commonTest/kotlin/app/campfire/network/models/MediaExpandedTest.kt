// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class MediaExpandedTest {

  @Test
  fun expandedEbookOnlyMedia_resolvesEbookFormatFromEbookFile() {
    // The expanded shape has no flat numTracks/ebookFormat fields — the format
    // lives on the ebookFile object and counts come from the arrays.
    val json = """
      {
        "id": "media_id",
        "libraryItemId": "item_id",
        "coverPath": null,
        "tags": [],
        "metadata": { "title": "An eBook" },
        "audioFiles": [],
        "chapters": [],
        "tracks": [],
        "ebookFile": {
          "ino": "ino_1",
          "metadata": {
            "filename": "book.epub",
            "ext": ".epub",
            "path": "/books/book.epub",
            "relPath": "book.epub",
            "size": 1024,
            "mtimeMs": 0,
            "ctimeMs": 0,
            "birthtimeMs": 0
          },
          "ebookFormat": "epub",
          "addedAt": 0,
          "updatedAt": 0
        }
      }
    """.trimIndent()

    val media = TestJson.decodeFromString<MediaExpanded>(json)

    assertThat(media.ebookFormat).isNull()
    assertThat(media.resolvedEbookFormat).isEqualTo("epub")
    assertThat(media.resolvedNumTracks).isEqualTo(0)
    assertThat(media.resolvedNumAudioFiles).isEqualTo(0)
    assertThat(media.resolvedNumChapters).isEqualTo(0)
  }

  @Test
  fun expandedMedia_resolvesCountsFromArrays() {
    val json = """
      {
        "id": "media_id",
        "libraryItemId": "item_id",
        "coverPath": null,
        "tags": [],
        "metadata": { "title": "An Audiobook" },
        "audioFiles": [],
        "chapters": [
          { "id": 0, "start": 0.0, "end": 100.0, "title": "Chapter 1" },
          { "id": 1, "start": 100.0, "end": 200.0, "title": "Chapter 2" }
        ],
        "tracks": [
          {
            "index": 1,
            "startOffset": 0.0,
            "duration": 200.0,
            "title": "track.mp3",
            "contentUrl": "/hls/track.mp3",
            "mimeType": "audio/mpeg",
            "codec": "mp3",
            "metadata": {
              "filename": "track.mp3",
              "ext": ".mp3",
              "path": "/books/track.mp3",
              "relPath": "track.mp3",
              "size": 1024,
              "mtimeMs": 0,
              "ctimeMs": 0,
              "birthtimeMs": 0
            }
          }
        ],
        "ebookFile": null
      }
    """.trimIndent()

    val media = TestJson.decodeFromString<MediaExpanded>(json)

    assertThat(media.numTracks).isEqualTo(0)
    assertThat(media.resolvedNumTracks).isEqualTo(1)
    assertThat(media.resolvedNumChapters).isEqualTo(2)
    assertThat(media.resolvedEbookFormat).isNull()
  }

  @Test
  fun minifiedMedia_passesThroughFlatFields() {
    val json = """
      {
        "id": "media_id",
        "coverPath": null,
        "metadata": { "title": "An eBook" },
        "numTracks": 0,
        "numAudioFiles": 0,
        "numChapters": 0,
        "ebookFormat": "pdf"
      }
    """.trimIndent()

    val media = TestJson.decodeFromString<MediaMinified>(json)

    assertThat(media.resolvedNumTracks).isEqualTo(0)
    assertThat(media.resolvedEbookFormat).isEqualTo("pdf")
  }
}
