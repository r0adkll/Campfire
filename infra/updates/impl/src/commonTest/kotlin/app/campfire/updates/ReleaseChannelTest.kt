// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ReleaseChannelTest {

  @Test
  fun `reads Drop's channel names`() {
    assertThat(ReleaseChannel.parse("main")).isEqualTo(ReleaseChannel.Main)
    assertThat(ReleaseChannel.parse("beta")).isEqualTo(ReleaseChannel.Beta)
    assertThat(ReleaseChannel.parse("pr/1290")).isEqualTo(ReleaseChannel.PullRequest(1290))
  }

  @Test
  fun `keeps names it doesn't know`() {
    assertThat(ReleaseChannel.parse("debug")).isEqualTo(ReleaseChannel.Other("debug"))
    assertThat(ReleaseChannel.parse("pr/next")).isEqualTo(ReleaseChannel.Other("pr/next"))
  }
}
