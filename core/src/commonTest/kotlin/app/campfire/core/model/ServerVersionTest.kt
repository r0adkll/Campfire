// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ServerVersionTest {

  @Test
  fun `parses plain semver`() {
    assertThat(ServerVersion.parse("2.22.0")).isEqualTo(ServerVersion(2, 22, 0))
  }

  @Test
  fun `parses v-prefixed version`() {
    assertThat(ServerVersion.parse("v2.34.1")).isEqualTo(ServerVersion(2, 34, 1))
  }

  @Test
  fun `parses git-describe style suffix`() {
    assertThat(ServerVersion.parse("2.34.0-4-ge39e8d8c")).isEqualTo(ServerVersion(2, 34, 0))
  }

  @Test
  fun `missing patch defaults to zero`() {
    assertThat(ServerVersion.parse("2.22")).isEqualTo(ServerVersion(2, 22, 0))
  }

  @Test
  fun `garbage returns null`() {
    assertThat(ServerVersion.parse("unknown")).isNull()
    assertThat(ServerVersion.parse("")).isNull()
    assertThat(ServerVersion.parse("beta")).isNull()
  }

  @Test
  fun `single component returns null`() {
    assertThat(ServerVersion.parse("2")).isNull()
  }

  @Test
  fun `comparison orders by major minor patch`() {
    assertThat(ServerVersion(2, 22, 0) >= ServerVersion(2, 22, 0)).isTrue()
    assertThat(ServerVersion(2, 21, 9) >= ServerVersion(2, 22, 0)).isFalse()
    assertThat(ServerVersion(2, 34, 0) >= ServerVersion(2, 22, 0)).isTrue()
    assertThat(ServerVersion(3, 0, 0) >= ServerVersion(2, 99, 99)).isTrue()
    assertThat(ServerVersion(2, 22, 1) > ServerVersion(2, 22, 0)).isTrue()
  }
}
