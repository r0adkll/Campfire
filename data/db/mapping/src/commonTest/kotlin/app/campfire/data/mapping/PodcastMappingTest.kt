// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

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
}
