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
  fun `decodes every build config field`() {
    assertThat(BuildCommit.fromBuildConfig(1_795_492_399L, "220c0be", "Ship it"))
      .isEqualTo(BuildCommit(Instant.fromEpochSeconds(1_795_492_399L), "220c0be", "Ship it"))
  }

  @Test
  fun `zero time and empty strings mean unknown`() {
    assertThat(BuildCommit.fromBuildConfig(0L, "220c0be", ""))
      .isEqualTo(BuildCommit(time = null, sha = "220c0be", subject = null))
    assertThat(BuildCommit.fromBuildConfig(1_795_492_399L, "", " "))
      .isEqualTo(BuildCommit(Instant.fromEpochSeconds(1_795_492_399L), sha = null, subject = null))
  }

  @Test
  fun `nothing known is no commit at all`() {
    assertThat(BuildCommit.fromBuildConfig(0L, "", "")).isNull()
  }
}
