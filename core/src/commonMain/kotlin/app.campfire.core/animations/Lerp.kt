// Copyright 2018, Google LLC, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.core.animations

fun lerp(
  startValue: Float,
  endValue: Float,
  fraction: Float,
) = startValue + fraction * (endValue - startValue)
