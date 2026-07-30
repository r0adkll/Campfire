// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ai.ui.emptystate

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastRoundToInt
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.random.Random

sealed class EmitArea(val size: IntSize) {
  abstract fun randomPosition(): IntOffset

  @OptIn(ExperimentalAtomicApi::class)
  class Distributed(
    size: IntSize,
    minDistance: Float,
    maxParticles: Int,
  ) : EmitArea(size) {
    private val pointIndex = AtomicInt(0)
    private val points = generatePoissonDiskSamples(
      size = size.toSize(),
      minDistance = minDistance.toDouble(),
      k = maxParticles,
    )

    override fun randomPosition(): IntOffset {
      val index = pointIndex.fetchAndIncrement() % points.size
      return points[index].let {
        IntOffset(it.x.fastRoundToInt(), it.y.fastRoundToInt())
      }
    }
  }

  class Bottom(size: IntSize) : EmitArea(size) {
    override fun randomPosition(): IntOffset {
      return IntOffset(
        x = Random.nextInt(size.width),
        y = size.height,
      )
    }
  }
}
