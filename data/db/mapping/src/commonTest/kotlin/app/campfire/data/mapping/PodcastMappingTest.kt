// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.test.FakeUrlHydrator
import app.campfire.network.models.AudioFile
import app.campfire.network.models.AudioMetaTags
import app.campfire.network.models.FileMetadata
import app.campfire.network.models.Podcast
import app.campfire.network.models.PodcastEpisode
import app.campfire.network.models.PodcastMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class PodcastMappingTest {

  @Test
  fun blankReleaseDate_mapsToNull() {
    val metadata = PodcastMetadata(title = "Some Show", releaseDate = "").asDomainModel()

    assertThat(metadata.releaseDate).isNull()
  }

  @Test
  fun releaseDate_mapsVerbatim() {
    val metadata = PodcastMetadata(title = "Some Show", releaseDate = "2019").asDomainModel()

    assertThat(metadata.releaseDate).isEqualTo("2019")
  }

  @Test
  fun fractionalEpisodeLimits_truncate() {
    val podcast = Podcast(id = "pod_1", maxEpisodesToKeep = 2.5, maxNewEpisodesToDownload = 3.9)
      .asDomainModel(libraryItemId = "li_1", urlHydrator = FakeUrlHydrator())

    assertThat(podcast.maxEpisodesToKeep).isEqualTo(2)
    assertThat(podcast.maxNewEpisodesToDownload).isEqualTo(3)
  }

  @Test
  fun unprobedEpisodeDuration_mapsToZero() {
    val episode = PodcastEpisode(
      libraryItemId = "li_1",
      podcastId = "pod_1",
      id = "ep_1",
      title = "Episode 1",
      audioFile = AudioFile(
        index = 1,
        ino = "1234",
        metadata = FileMetadata(
          filename = "ep1.mp3",
          ext = ".mp3",
          path = "/podcasts/Some Show/ep1.mp3",
          relPath = "ep1.mp3",
          size = 1000,
          mtimeMs = 0,
          ctimeMs = 0,
          birthtimeMs = 0,
        ),
        addedAt = 0,
        updatedAt = 0,
        manuallyVerified = false,
        exclude = false,
        format = "MP2/3 (MPEG audio layer 2/3)",
        chapters = emptyList(),
        metaTags = AudioMetaTags(),
        mimeType = "audio/mpeg",
      ),
      addedAt = 0,
      updatedAt = 0,
    )

    assertThat(episode.asDomainModel(FakeUrlHydrator()).durationInMillis).isEqualTo(0L)
    assertThat(episode.asDbModel(libraryItemId = "li_1", podcastMediaId = "pod_1").durationInMillis).isEqualTo(0L)
  }
}
