// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.network.models.AudioFile
import app.campfire.network.models.AudioMetaTags
import app.campfire.network.models.FileMetadata
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AudioFileMappingTest {

  @Test
  fun index_mapsVerbatim() {
    val audioFile = audioFile(index = 3)

    assertThat(audioFile.asDomainModel(fallbackIndex = 1).index).isEqualTo(3)
    assertThat(audioFile.asDbModel(MEDIA_ID, fallbackIndex = 1).mediaIndex).isEqualTo(3)
  }

  @Test
  fun missingIndex_mapsToFallback() {
    // The server leaves a rescanned file's index unset.
    val audioFile = audioFile(index = null)

    assertThat(audioFile.asDomainModel(fallbackIndex = 2).index).isEqualTo(2)
    assertThat(audioFile.asDbModel(MEDIA_ID, fallbackIndex = 2).mediaIndex).isEqualTo(2)
  }

  private fun audioFile(index: Int?) = AudioFile(
    index = index,
    ino = "1234",
    metadata = FileMetadata(
      filename = "track.mp3",
      ext = ".mp3",
      path = "/books/track.mp3",
      relPath = "track.mp3",
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
    duration = 60f,
    bitRate = 128000,
    codec = "mp3",
    timeBase = "1/14112000",
    channels = 2,
    channelLayout = "stereo",
    chapters = emptyList(),
    metaTags = AudioMetaTags(),
    mimeType = "audio/mpeg",
  )

  private companion object {
    const val MEDIA_ID = "media_1"
  }
}
