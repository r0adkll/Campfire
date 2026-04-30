package app.campfire.ui.theming.ui.ai.emptystate.components

import androidx.compose.ui.geometry.Offset
import com.r0adkll.cadence.game.ecs.Component

/**
 * This is attached to an entity to give it emission information
 * such as its exit velocity and acceleration. This component
 * allows us to act upon the entity in a particular system or not.
 */
class ParticleEmission(
  val exitVelocity: Offset,
  val exitAcceleration: Offset,
  val initialAngularVelocity: Float,
  val exitAngularVelocity: Float,
) : Component
