// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.campfire.ui.theming.ai.ui.emptystate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.ai.ui.emptystate.ShapeParticleGameState.Emitting
import app.campfire.ui.theming.ai.ui.emptystate.ShapeParticleGameState.End
import app.campfire.ui.theming.ai.ui.emptystate.ShapeParticleGameState.Idle
import app.campfire.ui.theming.ai.ui.emptystate.components.ParticleEmission
import app.campfire.ui.theming.ai.ui.emptystate.components.ShapeParticle
import app.campfire.ui.theming.ai.ui.emptystate.systems.ParticleEmissionSystem
import app.campfire.ui.theming.ai.ui.emptystate.systems.ParticleGraveyardSystem
import app.campfire.ui.theming.ai.ui.emptystate.systems.ShapeEmitterSystem
import com.r0adkll.cadence.game.components.RigidBody
import com.r0adkll.cadence.game.components.Transform
import com.r0adkll.cadence.game.rememberGameWorld
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val MinimumDistance = 50.dp
private const val MaxParticles = 50
private val DefaultInterval = 50.milliseconds
private val DefaultBackfillInterval = 5.milliseconds
private val DefaultSizeRange = 15.dp..40.dp

@Composable
fun ShapeParticleEmptyState(
  modifier: Modifier = Modifier,
  state: ShapeParticleGameState = Idle,
  maxParticles: Int = MaxParticles,
  minDistance: Dp = MinimumDistance,
  interval: Duration = DefaultInterval,
  backfillInterval: Duration = DefaultBackfillInterval,
  sizes: ClosedRange<Dp> = DefaultSizeRange,
) {
  var isTestEmitting by remember { mutableStateOf(false) }
  Box(
    modifier = modifier
      .clickable {
        isTestEmitting = !isTestEmitting
      },
  ) {
    ShapeParticleGameWorld(
      maxParticles = maxParticles,
      minDistance = minDistance,
      interval = interval,
      backfillInterval = backfillInterval,
      state = state,
      sizes = sizes,
    )
  }
}

@Composable
private fun ShapeParticleGameWorld(
  maxParticles: Int,
  minDistance: Dp,
  interval: Duration,
  backfillInterval: Duration,
  sizes: ClosedRange<Dp>,
  modifier: Modifier = Modifier,
  state: ShapeParticleGameState = Idle,
) {
  val density = LocalDensity.current
  val minimumDistance = with(density) { minDistance.toPx() }
  val emissionSystem = remember { ParticleEmissionSystem() }
  val shapeEmitter = remember {
    ShapeEmitterSystem(
      maxParticles = maxParticles,
      interval = interval,
      backfillInterval = backfillInterval,
      minimumDistance = minimumDistance,
      sizes = sizes,
    )
  }

  val gameWorld = rememberGameWorld {
    world.tracer = CadenceTracer()

    world.registerSystem(shapeEmitter)
    world.registerSystem(emissionSystem) {
      require<ParticleEmission>()
      require<RigidBody>()
    }
    world.registerSystem(ParticleGraveyardSystem(density)) {
      require<Transform>()
      require<ShapeParticle>()
    }
  }

  // Update our systems on whether or not they should be emitting
  // particles and making them flow to the top
  LaunchedEffect(state) {
    shapeEmitter.state = when (state) {
      Emitting -> ShapeEmitterSystem.State.Emitting
      End -> ShapeEmitterSystem.State.Disabled
      Idle -> ShapeEmitterSystem.State.Idle
    }
    emissionSystem.setEmitting(state is Emitting || state is End)
  }

  // Render and run the game world
  gameWorld.Content(
    modifier.fillMaxSize(),
  )
}

sealed interface ShapeParticleGameState {
  data object Idle : ShapeParticleGameState
  data object Emitting : ShapeParticleGameState
  data object End : ShapeParticleGameState
}
