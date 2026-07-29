package app.campfire.ui.theming.ai.ui.emptystate.systems

import androidx.compose.ui.geometry.Offset
import app.campfire.ui.theming.ai.ui.emptystate.components.ParticleEmission
import com.r0adkll.cadence.game.components.RigidBody
import com.r0adkll.cadence.game.ecs.System
import com.r0adkll.cadence.tracer.trace

/**
 * An ECS system to act upon entities that have [ParticleEmission] and
 * [RigidBody] components
 */
class ParticleEmissionSystem() : System() {

  private var enabled = false

  /**
   * Set the entities in this system as emitting, or not.
   */
  fun setEmitting(enabled: Boolean) = world.tracer.trace("ParticleEmissionSystem.setEmitting($enabled)") {
    this.enabled = enabled

    // For each entity, set its velocity/acceleration to the values
    // defined in its [ParticleEmission] component.
    entities.forEach { entity ->
      val emission = world.getComponent<ParticleEmission>(entity)!!
      val rigidBody = world.getComponent<RigidBody>(entity)!!

      rigidBody.velocity = if (enabled) emission.exitVelocity else Offset.Zero
      rigidBody.acceleration = if (enabled) emission.exitAcceleration else Offset.Zero
      rigidBody.angularVelocity = if (enabled) emission.exitAngularVelocity else emission.initialAngularVelocity
    }
  }

  override fun update(timeNanos: Long, deltaNs: Long, delta: Double) {
    if (!enabled) return

    world.tracer.trace("ParticleEmissionSystem.update") {
      // Ensure that each entity in the system is actually "emitting"
      entities.forEach { entity ->
        val emission = world.getComponent<ParticleEmission>(entity)!!
        val rigidBody = world.getComponent<RigidBody>(entity)!!

        if (rigidBody.velocity == Offset.Zero) {
          rigidBody.velocity = emission.exitVelocity
          rigidBody.acceleration = emission.exitAcceleration
          rigidBody.angularVelocity = emission.exitAngularVelocity
        }
      }
    }
  }
}
