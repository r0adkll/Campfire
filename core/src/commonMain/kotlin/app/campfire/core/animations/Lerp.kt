// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.animations

fun lerp(
  startValue: Float,
  endValue: Float,
  fraction: Float,
) = startValue + fraction * (endValue - startValue)
