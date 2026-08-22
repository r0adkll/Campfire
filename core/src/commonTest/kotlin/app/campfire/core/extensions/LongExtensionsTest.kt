// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import app.cash.burst.Burst
import app.cash.burst.burstValues
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

@Burst
class LongExtensionsTest {

  @Test
  fun asReadableBytes(
    case: Pair<Long, String> = burstValues(
      Pair(0L, "0 B"),
      Pair(1023L, "1023 B"),
      Pair(1024L, "1.0 KB"),
      Pair(1536L, "1.5 KB"),
      Pair(1024L * 1024L, "1.0 MB"),
      Pair(10L * 1024L * 1024L, "10.0 MB"),
      Pair((1.5 * 1024 * 1024).toLong(), "1.5 MB"),
      Pair((1.75 * 1024 * 1024 * 1024).toLong(), "1.8 GB"),
      Pair(2L * 1024L * 1024L * 1024L * 1024L, "2.0 TB"),
    ),
  ) {
    val (bytes, expected) = case
    assertThat(bytes.asReadableBytes()).isEqualTo(expected)
  }
}
