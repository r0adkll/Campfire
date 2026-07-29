package app.campfire.ui.theming.ai.ui.emptystate.systems

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.graphics.shapes.RoundedPolygon
import app.campfire.ui.theming.ai.ui.emptystate.EmitArea
import app.campfire.ui.theming.ai.ui.emptystate.components.ParticleEmission
import app.campfire.ui.theming.ai.ui.emptystate.components.ShapeParticle
import com.r0adkll.cadence.game.components.Gravity
import com.r0adkll.cadence.game.components.Renderable
import com.r0adkll.cadence.game.components.RigidBody
import com.r0adkll.cadence.game.components.Transform
import com.r0adkll.cadence.game.ecs.System
import com.r0adkll.cadence.tracer.trace
import kotlin.random.Random
import kotlin.time.Duration

/**
 * The [ShapeParticle] emitter system. This will initially emit [maxParticles] amounts
 * on first instance. Then, if enabled, will emit random shape particles @ [interval].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ShapeEmitterSystem(
  val maxParticles: Int,
  interval: Duration,
  backfillInterval: Duration,
  private val minimumDistance: Float,
  private val sizes: ClosedRange<Dp> = 15.dp..40.dp,
  private val alpha: ClosedRange<Float> = 0.25f..0.5f,
  private val numDepths: Int = 3,
  private val angularVelocities: ClosedRange<Float> = -50f..50f,
  private val exitVelocities: ClosedRange<Double> = 200.0..400.0,
  private val exitAcceleration: ClosedRange<Double> = 100.0..500.0,
  private val possibleShapes: List<RoundedPolygon> = listOf(
    MaterialShapes.Circle,
    MaterialShapes.Square,
    MaterialShapes.Slanted,
    MaterialShapes.Arch,
    MaterialShapes.Fan,
    MaterialShapes.Arrow,
    MaterialShapes.SemiCircle,
    MaterialShapes.Oval,
    MaterialShapes.Pill,
    MaterialShapes.Triangle,
    MaterialShapes.Diamond,
    MaterialShapes.ClamShell,
    MaterialShapes.Pentagon,
    MaterialShapes.Gem,
    MaterialShapes.Sunny,
    MaterialShapes.VerySunny,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Cookie7Sided,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Cookie12Sided,
    MaterialShapes.Ghostish,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Clover8Leaf,
    MaterialShapes.Burst,
    MaterialShapes.SoftBurst,
    MaterialShapes.Boom,
    MaterialShapes.SoftBoom,
    MaterialShapes.Flower,
    MaterialShapes.Puffy,
    MaterialShapes.PuffyDiamond,
    MaterialShapes.PixelCircle,
    MaterialShapes.PixelTriangle,
    MaterialShapes.Bun,
    MaterialShapes.Heart,
  ),
) : System() {

  enum class State {
    Idle,
    Emitting,
    Disabled,
  }

  private val intervalNs = interval.inWholeNanoseconds
  private var cumulativeTime: Long = 0L

  var state: State = State.Idle
    set(value) {
      field = value
      if (value == State.Emitting) {
        cumulativeTime = 0L
      } else if (value == State.Idle) {
        backfillCumulativeTime = 0L
      }
    }

  private lateinit var area: EmitArea
  private var hasInitialized = false
  private var backfillCumulativeTime: Long = 0L
  private val backfillIntervalNs = backfillInterval.inWholeNanoseconds

  override fun update(timeNanos: Long, deltaNs: Long, delta: Double) = world.tracer.trace("ShapeEmitterSystem.update") {
    val window = world.getWindow()

    // Perform the initial emission
    if (
      state == State.Idle &&
      world.entityManager.living < maxParticles &&
      window.size != IntSize.Zero
    ) {
      if (!::area.isInitialized) {
        area = world.tracer.trace("ShapeEmitterSystem.generatePoissonDiskSamples") {
          EmitArea.Distributed(
            size = window.size,
            minDistance = minimumDistance,
            maxParticles = maxParticles + 15,
          )
        }
      }

      if (!hasInitialized) {
        repeat(maxParticles) {
          emit(area)
        }
        hasInitialized = true
      }

      backfillCumulativeTime += deltaNs
      if (backfillCumulativeTime > backfillIntervalNs) {
        emit(area)
        backfillCumulativeTime = 0L
      }
    }

    // If emitting is not enabled, then just stop here
    // We don't want to continuously emit entities if not enabled.
    if (state != State.Emitting) return@trace

    cumulativeTime += deltaNs
    if (cumulativeTime > intervalNs) {
      emit(EmitArea.Bottom(window.size))
      cumulativeTime = 0L
    }
  }

  private fun emit(area: EmitArea) = world.tracer.trace("ShapeEmitterSystem.emit") {
    val sizeStep = (sizes.endInclusive - sizes.start) / numDepths.toFloat()
    val alphaStep = (alpha.endInclusive - alpha.start) / numDepths.toFloat()
    val depth = Random.nextInt(numDepths)
    val position = area.randomPosition()
    val angularVelocity =
      angularVelocities.start + ((angularVelocities.endInclusive - angularVelocities.start) * Random.nextFloat())

    val exitVelocity =
      exitVelocities.start + ((exitVelocities.endInclusive - exitVelocities.start) * Random.nextFloat())
    val exitAcceleration =
      exitAcceleration.start + ((exitAcceleration.endInclusive - exitAcceleration.start) * Random.nextFloat())

    world.createEntity {
      // These three make the entity engage with the
      // physics system
      addComponent(Transform(initialPosition = position.toOffset()))
      addComponent(RigidBody(angularVelocity = angularVelocity))
      addComponent(Gravity())

      // This makes it so we can turn the velocity on/off based
      // on emission state
      addComponent(
        ParticleEmission(
          exitVelocity = Offset(0f, -exitVelocity.toFloat()),
          exitAcceleration = Offset(0f, -exitAcceleration.toFloat()),
          initialAngularVelocity = angularVelocity,
          exitAngularVelocity = angularVelocity * (5f + (Random.nextFloat() * 5f)),
        ),
      )

      // This component lets us render the particle per-entity
      val particle = ShapeParticle(
        shape = possibleShapes.random(),
        alpha = alpha.start + (alphaStep * depth),
        size = sizes.start + (sizeStep * depth),
      )
      addComponent(particle)
      // A sort of inheritence hack for this system to
      // share a rendering state with other logic/vars
      addComponent(particle as Renderable)
    }
  }
}
