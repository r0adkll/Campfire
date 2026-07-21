package app.campfire.settings

import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.api.rewindForPause
import app.campfire.settings.api.tiers
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class ResumeRewindTierTest {

  private val settingsScope = CoroutineScope(Dispatchers.Unconfined + Job())

  @Test
  fun `tiers span from minRewind to maxRewind and increase monotonically`() {
    val config = ResumeRewindConfig(
      minPauseThreshold = 5.seconds,
      minRewind = 5.seconds,
      maxRewind = 2.minutes,
    )
    val tiers = config.tiers()

    assertThat(tiers.map { it.pauseThreshold })
      .containsExactly(5.seconds, 1.minutes, 5.minutes, 15.minutes, 30.minutes)
    // First tier == minRewind, last tier == maxRewind
    assertThat(tiers.first().rewindAmount).isEqualTo(5.seconds)
    assertThat(tiers.last().rewindAmount).isEqualTo(2.minutes)
    // Strictly increasing rewind — impossible to get a non-monotonic window
    val increasing = tiers.map { it.rewindAmount }.zipWithNext().all { (a, b) -> b > a }
    assertThat(increasing).isTrue()
  }

  @Test
  fun `rewindForPause returns zero below the min pause threshold`() {
    val config = ResumeRewindConfig(5.seconds, 5.seconds, 2.minutes)
    assertThat(config.rewindForPause(4.seconds)).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `rewindForPause picks the highest tier at or below the pause duration`() {
    val config = ResumeRewindConfig(5.seconds, 5.seconds, 2.minutes)
    // At and just below the first real tier -> minRewind
    assertThat(config.rewindForPause(5.seconds)).isEqualTo(5.seconds)
    assertThat(config.rewindForPause(59.seconds)).isEqualTo(5.seconds)
    // The top tier caps at maxRewind
    assertThat(config.rewindForPause(45.minutes)).isEqualTo(2.minutes)
  }

  @Test
  fun `resumeRewindConfig defaults and round-trips through storage`() {
    val settings = playbackSettings()
    assertThat(settings.resumeRewindConfig).isEqualTo(ResumeRewindConfig.Default)

    val custom = ResumeRewindConfig(
      minPauseThreshold = 2.seconds,
      minRewind = 10.seconds,
      maxRewind = 90.seconds,
    )
    settings.resumeRewindConfig = custom
    assertThat(settings.resumeRewindConfig).isEqualTo(custom)
  }

  @Test
  fun `pendingResumeRewind round-trips and can be cleared`() {
    val settings = playbackSettings()
    assertThat(settings.pendingResumeRewind).isNull()

    val pending = PendingResumeRewind(pausedAtEpochMillis = 1_700_000_000_000L, libraryItemId = "li_abc123")
    settings.pendingResumeRewind = pending
    assertThat(settings.pendingResumeRewind).isEqualTo(pending)

    settings.pendingResumeRewind = null
    assertThat(settings.pendingResumeRewind).isNull()
  }

  private fun playbackSettings(): PlaybackSettingsImpl =
    PlaybackSettingsImpl(MapSettings(), settingsScope)
}
