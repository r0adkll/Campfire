// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.navigation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class OpenBuildLinkTest {

  @Test
  fun `reads the build ID from an Open in App link`() {
    assertThat(OpenBuildLink.parseBuildId("campfireaudiobooks://drop/aB3dE6gH")).isEqualTo("aB3dE6gH")
  }

  @Test
  fun `matches the scheme and host apart from case`() {
    assertThat(OpenBuildLink.parseBuildId("CampfireAudiobooks://DROP/aB3dE6gH")).isEqualTo("aB3dE6gH")
  }

  @Test
  fun `allows a trailing slash and surrounding whitespace`() {
    assertThat(OpenBuildLink.parseBuildId(" campfireaudiobooks://drop/aB3dE6gH/ ")).isEqualTo("aB3dE6gH")
  }

  @Test
  fun `other links aren't Open in App links`() {
    listOf(
      "campfireaudiobooks://connect?v=1",
      "campfireaudiobooks://oauth/aB3dE6gH",
      "https://drop/aB3dE6gH",
      "drop/aB3dE6gH",
      "campfireaudiobooks://drop.example.com/aB3dE6gH",
    ).forEach { url ->
      assertThat(OpenBuildLink.parseBuildId(url), url).isNull()
    }
  }

  @Test
  fun `a link without exactly one build ID isn't one`() {
    listOf(
      "campfireaudiobooks://drop",
      "campfireaudiobooks://drop/",
      "campfireaudiobooks://drop/aB3dE6gH/extra",
      "campfireaudiobooks://drop/aB3dE6gH?open=failed",
      "campfireaudiobooks://drop/aB3dE6gH#fragment",
      "campfireaudiobooks://drop/../aB3dE6gH",
      "campfireaudiobooks://drop/{build}",
    ).forEach { url ->
      assertThat(OpenBuildLink.parseBuildId(url), url).isNull()
    }
  }
}
