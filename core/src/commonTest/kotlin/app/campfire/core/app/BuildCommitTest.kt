// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Instant

class BuildCommitTest {

  @Test
  fun `decodes both build config fields`() {
    assertThat(BuildCommit.fromBuildConfig(1_795_492_399L, "220c0be"))
      .isEqualTo(BuildCommit(Instant.fromEpochSeconds(1_795_492_399L), "220c0be"))
  }

  @Test
  fun `zero time and empty sha mean unknown`() {
    assertThat(BuildCommit.fromBuildConfig(0L, "220c0be")).isEqualTo(BuildCommit(time = null, sha = "220c0be"))
    assertThat(BuildCommit.fromBuildConfig(1_795_492_399L, ""))
      .isEqualTo(BuildCommit(Instant.fromEpochSeconds(1_795_492_399L), sha = null))
  }

  @Test
  fun `nothing known is no commit at all`() {
    assertThat(BuildCommit.fromBuildConfig(0L, "")).isNull()
  }
}
