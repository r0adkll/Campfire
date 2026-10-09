// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.account.test.FakeUrlHydrator
import app.campfire.network.models.AudioFile
import app.campfire.network.models.AudioMetaTags
import app.campfire.network.models.AudioTrack
import app.campfire.network.models.FileMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration

class AudioFileMappingTest {

  @Test
  fun unprobedAudioFile_mapsToDefaults() {
    val domain = unprobedAudioFile.asDomainModel()

    assertThat(domain.duration).isEqualTo(Duration.ZERO)
    assertThat(domain.bitRate).isEqualTo(0)
    assertThat(domain.codec).isEqualTo("")
    assertThat(domain.timeBase).isEqualTo("")
    assertThat(domain.channels).isEqualTo(0)
    assertThat(domain.channelLayout).isEqualTo("")
  }

  @Test
  fun unprobedAudioFile_storesDefaults() {
    val row = unprobedAudioFile.asDbModel(mediaId = "media_1")

    assertThat(row.duration).isEqualTo(0.0)
    assertThat(row.bitRate).isEqualTo(0)
    assertThat(row.codec).isEqualTo("")
    assertThat(row.timeBase).isEqualTo("")
    assertThat(row.channels).isEqualTo(0)
    assertThat(row.channelLayout).isEqualTo("")
  }

  @Test
  fun unprobedAudioTrack_mapsToDefaults() {
    val domain = unprobedAudioTrack.asDomainModel(FakeUrlHydrator())

    assertThat(domain.duration).isEqualTo(0f)
    assertThat(domain.codec).isEqualTo("")
  }

  @Test
  fun unprobedAudioTrack_storesDefaults() {
    val bookRow = unprobedAudioTrack.asDbModel(mediaId = "media_1")
    val episodeRow = unprobedAudioTrack.asEpisodeDbModel(episodeId = "ep_1")

    assertThat(bookRow.duration).isEqualTo(0.0)
    assertThat(bookRow.codec).isEqualTo("")
    assertThat(episodeRow.duration).isEqualTo(0.0)
    assertThat(episodeRow.codec).isEqualTo("")
  }

  private val fileMetadata = FileMetadata(
    filename = "ep1.mp3",
    ext = ".mp3",
    path = "/podcasts/Some Show/ep1.mp3",
    relPath = "ep1.mp3",
    size = 1000,
    mtimeMs = 0,
    ctimeMs = 0,
    birthtimeMs = 0,
  )

  private val unprobedAudioFile = AudioFile(
    index = 1,
    ino = "1234",
    metadata = fileMetadata,
    addedAt = 0,
    updatedAt = 0,
    manuallyVerified = false,
    exclude = false,
    format = "MP2/3 (MPEG audio layer 2/3)",
    chapters = emptyList(),
    metaTags = AudioMetaTags(),
    mimeType = "audio/mpeg",
  )

  private val unprobedAudioTrack = AudioTrack(
    index = 1,
    startOffset = 0f,
    title = "ep1.mp3",
    contentUrl = "/api/items/li_1/file/1234",
    mimeType = "audio/mpeg",
    metadata = fileMetadata,
  )
}
