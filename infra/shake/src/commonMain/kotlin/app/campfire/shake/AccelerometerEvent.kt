// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.shake

data class AccelerometerEvent(
  val x: Double,
  val y: Double,
  val z: Double,
  val timestamp: Long,
)
