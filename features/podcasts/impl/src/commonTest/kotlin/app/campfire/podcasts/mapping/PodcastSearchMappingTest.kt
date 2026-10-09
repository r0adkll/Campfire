// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.mapping

import app.campfire.podcasts.api.PodcastDraft
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class PodcastSearchMappingTest {

  @Test
  fun isoReleaseDate_isSent() {
    val metadata = draft(releaseDateIso = "2026-05-15T13:05:19Z").asCreateMetadata()

    assertThat(metadata.releaseDate).isEqualTo("2026-05-15T13:05:19Z")
  }

  @Test
  fun feedPubDate_isDropped() {
    val metadata = draft(releaseDateIso = "Fri, 15 May 2026 13:05:19 GMT").asCreateMetadata()

    assertThat(metadata.releaseDate).isNull()
  }

  private fun draft(releaseDateIso: String?) = PodcastDraft(
    title = "My Podcast",
    author = null,
    descriptionHtml = null,
    descriptionPlain = null,
    coverUrl = null,
    feedUrl = "https://example.com/feed.xml",
    itunesId = null,
    itunesArtistId = null,
    itunesPageUrl = null,
    releaseDateIso = releaseDateIso,
    language = null,
    genres = emptyList(),
    explicit = false,
  )
}
