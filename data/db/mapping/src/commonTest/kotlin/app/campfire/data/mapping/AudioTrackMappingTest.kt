// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.api.UrlHydrator
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItemId
import app.campfire.network.models.AudioTrack as NetworkAudioTrack
import app.campfire.network.models.FileMetadata as NetworkFileMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AudioTrackMappingTest {

  private val hydrator = PrefixUrlHydrator("https://abs.example.com")

  private val networkTrack = NetworkAudioTrack(
    index = 1,
    startOffset = 0f,
    duration = 1200f,
    title = "episode.mp3",
    contentUrl = "/api/items/li_1/file/42",
    mimeType = "audio/mpeg",
    codec = "mp3",
    metadata = NetworkFileMetadata(
      filename = "episode.mp3",
      ext = ".mp3",
      path = "/podcasts/Show/episode.mp3",
      relPath = "episode.mp3",
      size = 1_000,
      mtimeMs = 1,
      ctimeMs = 2,
      birthtimeMs = 3,
    ),
  )

  @Test
  fun episodeTrack_storesTheServersPaths() {
    val row = networkTrack.asDomainModel(hydrator).asEpisodeDbModel("ep_1", hydrator)

    assertThat(row.contentUrl).isEqualTo("/api/items/li_1/file/42")
    assertThat(row.metadata_path).isEqualTo("/podcasts/Show/episode.mp3")
  }

  @Test
  fun episodeTrack_readsBackAsItWasWritten() {
    val track = networkTrack.asDomainModel(hydrator)

    val readBack = track.asEpisodeDbModel("ep_1", hydrator).asDomainModel(hydrator)

    assertThat(readBack).isEqualTo(track)
    assertThat(readBack.contentUrl).isEqualTo("https://abs.example.com/api/items/li_1/file/42")
  }

  @Test
  fun bookTrack_storesTheServersPaths() {
    val row = networkTrack.asDomainModel(hydrator).asDbModel("media_1", hydrator)

    assertThat(row.contentUrl).isEqualTo("/api/items/li_1/file/42")
    assertThat(row.metadata_path).isEqualTo("/podcasts/Show/episode.mp3")
  }
}

private class PrefixUrlHydrator(private val serverUrl: String) : UrlHydrator {
  override fun hydrateUrl(absolutePath: String) = "$serverUrl$absolutePath"
  override fun dehydrateUrl(url: String) = url.removePrefix(serverUrl)
  override fun hydrateLibraryItem(libraryItemId: LibraryItemId, updatedAtMillis: Long?) = "$serverUrl/cover"
  override fun hydrateAuthor(authorId: AuthorId, updatedAtMillis: Long?) = "$serverUrl/author"
}
