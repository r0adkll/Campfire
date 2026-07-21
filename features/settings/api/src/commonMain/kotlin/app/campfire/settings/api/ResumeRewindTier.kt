package app.campfire.settings.api

import app.campfire.core.model.LibraryItemId
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A single, derived step in the "auto-rewind on resume" sliding window: resuming after a pause of at least
 * [pauseThreshold] rewinds playback by [rewindAmount]. Tiers are computed from a [ResumeRewindConfig] — they
 * are not configured directly — so the window is always monotonic (longer pauses never rewind less).
 */
data class ResumeRewindTier(
  val pauseThreshold: Duration,
  val rewindAmount: Duration,
)

/**
 * The user-facing configuration for auto-rewind on resume.
 *
 * Rather than letting the user hand-edit each tier (which allows nonsensical, non-monotonic windows), the user
 * picks a floor and a rewind range; the tiers are then interpolated across a fixed set of pause checkpoints:
 *
 * @property minPauseThreshold pauses shorter than this rewind nothing. Constrained to [MinPauseThresholdRange].
 * @property minRewind the rewind applied at the shortest qualifying pause (the [minPauseThreshold] checkpoint).
 * @property maxRewind the rewind applied at the longest pause checkpoint ([ResumeRewindPauseTiers] last entry).
 */
data class ResumeRewindConfig(
  val minPauseThreshold: Duration,
  val minRewind: Duration,
  val maxRewind: Duration,
) {
  companion object {
    val Default = ResumeRewindConfig(
      minPauseThreshold = 5.seconds,
      minRewind = 5.seconds,
      maxRewind = 2.minutes,
    )
  }
}

/** The allowed range for [ResumeRewindConfig.minPauseThreshold]. */
val MinPauseThresholdRange: ClosedRange<Duration> = Duration.ZERO..10.seconds

/** The allowed range for the rewind amount (both ends of the [ResumeRewindConfig] range slider). */
val ResumeRewindRange: ClosedRange<Duration> = Duration.ZERO..5.minutes

/**
 * The fixed pause-duration checkpoints, above the configurable [ResumeRewindConfig.minPauseThreshold], across
 * which the rewind amount is scaled from [ResumeRewindConfig.minRewind] to [ResumeRewindConfig.maxRewind].
 */
val ResumeRewindPauseTiers: List<Duration> = listOf(
  1.minutes,
  5.minutes,
  15.minutes,
  30.minutes,
)

/**
 * Expand this config into its concrete, monotonically-increasing list of [ResumeRewindTier]s. The first tier
 * sits at [ResumeRewindConfig.minPauseThreshold] with [ResumeRewindConfig.minRewind]; the last sits at the
 * largest pause checkpoint with [ResumeRewindConfig.maxRewind]; the rest are linearly interpolated by index.
 */
fun ResumeRewindConfig.tiers(): List<ResumeRewindTier> {
  val thresholds = buildList {
    add(minPauseThreshold)
    addAll(ResumeRewindPauseTiers.filter { it > minPauseThreshold })
  }
  val steps = thresholds.size
  val minSeconds = minRewind.inWholeSeconds
  val maxSeconds = maxRewind.inWholeSeconds
  return thresholds.mapIndexed { index, threshold ->
    val fraction = if (steps <= 1) 1.0 else index.toDouble() / (steps - 1)
    val rewindSeconds = (minSeconds + (maxSeconds - minSeconds) * fraction).roundToLong()
    ResumeRewindTier(threshold, rewindSeconds.seconds)
  }
}

/**
 * Resolve the rewind amount for a given [pauseDuration]. Returns [Duration.ZERO] when the pause is shorter than
 * [ResumeRewindConfig.minPauseThreshold]; otherwise the amount from the highest matching tier.
 */
fun ResumeRewindConfig.rewindForPause(pauseDuration: Duration): Duration {
  if (pauseDuration < minPauseThreshold) return Duration.ZERO
  return tiers().lastOrNull { pauseDuration >= it.pauseThreshold }?.rewindAmount ?: Duration.ZERO
}

/**
 * A persisted marker for a pause that may still owe a rewind when playback resumes. Persisting this — rather
 * than holding it in memory — means a pause interrupted by the app being killed still rewinds on resume.
 *
 * @property pausedAtEpochMillis wall-clock time the pause began.
 * @property libraryItemId the item that was paused; a resume only rewinds when it matches, so starting a
 *   different item (or a fresh session) never triggers a spurious rewind.
 */
data class PendingResumeRewind(
  val pausedAtEpochMillis: Long,
  val libraryItemId: LibraryItemId,
)
