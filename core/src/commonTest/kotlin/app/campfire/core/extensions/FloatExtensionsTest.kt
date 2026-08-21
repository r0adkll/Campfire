// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import app.cash.burst.Burst
import app.cash.burst.burstValues
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

@Burst
class FloatExtensionsTest {

  @Test
  fun toStringDecimalPlaces(
    case: Triple<Float, Int, String> = burstValues(
      Triple(0f, 0, "0"),
      Triple(0f, 2, "0.00"),
      Triple(10f, 1, "10.0"),
      Triple(1.03f, 2, "1.03"),
      Triple(1.1f, 2, "1.10"),
      Triple(1.25f, 2, "1.25"),
      Triple(1.999f, 2, "2.00"),
      Triple(0.96f, 1, "1.0"),
      Triple(23.35f, 1, "23.4"),
      Triple(485.1234f, 3, "485.123"),
      Triple(-128.9876f, 2, "-128.99"),
      Triple(-0.001f, 2, "0.00"),
      Triple(Float.NaN, 2, "--"),
      Triple(Float.POSITIVE_INFINITY, 1, "--"),
    ),
  ) {
    val (value, decimals, expected) = case
    assertThat(value.toString(decimals)).isEqualTo(expected)
  }

  @Test
  fun readableHundredths(
    case: Pair<Float, String> = burstValues(
      0.5f to "0.5",
      1f to "1",
      1.01f to "1.01",
      1.03f to "1.03",
      1.1f to "1.1",
      1.10f to "1.1",
      1.12f to "1.12",
      1.2f to "1.2",
      1.25f to "1.25",
      1.1500001f to "1.15",
      2f to "2",
    ),
  ) {
    val (value, expected) = case
    assertThat(value.readableHundredths).isEqualTo(expected)
  }

  @Test
  fun readable(
    case: Pair<Float, String> = burstValues(
      0.5f to "0.5",
      1f to "1",
      1.1f to "1.1",
      1.2f to "1.2",
      1.25f to "1.25",
      1.75f to "1.75",
      1.23f to "1.2",
      2f to "2",
    ),
  ) {
    val (value, expected) = case
    assertThat(value.readable).isEqualTo(expected)
  }

  @Test
  fun roundToHundredths(
    case: Pair<Float, Float> = burstValues(
      1.1500001f to 1.15f,
      1.2049f to 1.2f,
      1.205f to 1.21f,
      0.5f to 0.5f,
    ),
  ) {
    val (value, expected) = case
    assertThat(value.roundToHundredths()).isEqualTo(expected)
  }
}
