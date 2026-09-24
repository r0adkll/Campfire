// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UnreachableServerBackoffTest {

  @Test
  fun `reachable server keeps the base interval`() {
    val backoff = UnreachableServerBackoff()

    assertThat(backoff.interval(15.seconds)).isEqualTo(15.seconds)
  }

  @Test
  fun `each unreachable sync doubles the interval`() {
    val backoff = UnreachableServerBackoff()

    backoff.onUnreachable()
    assertThat(backoff.interval(15.seconds)).isEqualTo(30.seconds)
    backoff.onUnreachable()
    assertThat(backoff.interval(15.seconds)).isEqualTo(60.seconds)
    backoff.onUnreachable()
    assertThat(backoff.interval(15.seconds)).isEqualTo(120.seconds)
  }

  @Test
  fun `interval is capped`() {
    val backoff = UnreachableServerBackoff(cap = 5.minutes)

    repeat(40) { backoff.onUnreachable() }

    assertThat(backoff.interval(15.seconds)).isEqualTo(5.minutes)
  }

  @Test
  fun `a base interval above the cap is never shortened`() {
    val backoff = UnreachableServerBackoff(cap = 5.minutes)

    backoff.onUnreachable()

    assertThat(backoff.interval(10.minutes)).isEqualTo(10.minutes)
  }

  @Test
  fun `reaching the server restores the base interval`() {
    val backoff = UnreachableServerBackoff()
    repeat(3) { backoff.onUnreachable() }

    backoff.onReachable()

    assertThat(backoff.interval(15.seconds)).isEqualTo(15.seconds)
  }

  @Test
  fun `any unanswered request makes a pass unreachable`() {
    assertThat(ServerContact.Reached + ServerContact.Unreachable).isEqualTo(ServerContact.Unreachable)
    assertThat(ServerContact.NotAttempted + ServerContact.Reached).isEqualTo(ServerContact.Reached)
    assertThat(ServerContact.NotAttempted + ServerContact.NotAttempted).isEqualTo(ServerContact.NotAttempted)
  }
}
