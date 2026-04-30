package app.campfire.ui.theming.ui.ai.emptystate.systems

import androidx.compose.ui.unit.Density
import app.campfire.ui.theming.ui.ai.emptystate.components.ShapeParticle
import com.r0adkll.cadence.game.components.Transform
import com.r0adkll.cadence.game.ecs.Entity
import com.r0adkll.cadence.game.ecs.System
import com.r0adkll.cadence.tracer.trace

/**
 * A system that acts upon [Transform] and [ShapeParticle] components
 * to determine if an entity is "dead", i.e. out of screen and destroys it.
 */
class ParticleGraveyardSystem(
  private val density: Density,
) : System() {

  private var deadEntities = mutableSetOf<Entity>()

  override fun updatePhysics(timeNanos: Long, deltaNs: Long, delta: Double) =
    world.tracer.trace("ParticleGraveyardSystem.updatePhysics") {
      entities.forEach { entity ->
        val transform = world.getComponent<Transform>(entity)!!
        val particle = world.getComponent<ShapeParticle>(entity)!!

        val particleHeight = with(density) { particle.size.toPx() }

        if (transform.position.y + particleHeight < 0) {
          deadEntities += entity
        }
      }

      if (deadEntities.isNotEmpty()) {
        deadEntities.forEach { world.destroyEntity(it) }
        deadEntities.clear()
      }
    }
}
