// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.TestJson
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

class NumEpisodesIncompleteTest {

  @Test
  fun includedCount_decodes() {
    val item = TestJson.decodeFromString<LibraryItemMinified>(podcastItemJson(""""numEpisodesIncomplete": 7,"""))

    assertThat(item).isInstanceOf<LibraryItemMinified.Podcast>()
      .prop(LibraryItemMinified.Podcast::numEpisodesIncomplete)
      .isEqualTo(7)
  }

  @Test
  fun missingCount_decodesAsZero() {
    // The server leaves the field out when every episode is finished.
    val item = TestJson.decodeFromString<LibraryItemMinified>(podcastItemJson(""))

    assertThat(item).isInstanceOf<LibraryItemMinified.Podcast>()
      .prop(LibraryItemMinified.Podcast::numEpisodesIncomplete)
      .isEqualTo(0)
  }

  // The minified shape served by `/api/libraries/:id/items?minified=1&include=numEpisodesIncomplete`.
  private fun podcastItemJson(numEpisodesIncomplete: String) = """
    {
      "id": "li_1",
      "ino": "1234",
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
        "metadata": { "title": "Some Show" },
        "coverPath": null,
        "tags": [],
        "numEpisodes": 9,
        "size": 123456
      },
      $numEpisodesIncomplete
      "numFiles": 9,
      "size": 123456
    }
  """.trimIndent()
}
