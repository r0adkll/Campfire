// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.settings

import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.test.FakeEqualizerSettings
import app.campfire.settings.test.FakePlaybackSettings
import app.campfire.settings.test.FakeSleepSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSettingsSnapshotTest {

  private val playback = FakePlaybackSettings()
  private val sleep = FakeSleepSettings()

  private fun TestScope.snapshot() = PlayerSettingsSnapshot(
    playbackSettings = playback,
    equalizerSettings = FakeEqualizerSettings(),
    sleepSettings = sleep,
    scope = backgroundScope,
  )

  @Test
  fun `holds the defaults until the settings are read`() = runTest(StandardTestDispatcher()) {
    playback.setForwardTimeMs(45_000L)

    val snapshot = snapshot()

    assertThat(snapshot.forwardTime.value).isEqualTo(PlaybackSettings.DefaultForwardTime)
    runCurrent()
    assertThat(snapshot.forwardTime.value).isEqualTo(45.seconds)
  }

  @Test
  fun `follows changes to the settings`() = runTest(StandardTestDispatcher()) {
    val snapshot = snapshot()
    runCurrent()

    playback.setRemoteNextPrevSkipsChapters(true)
    sleep.setShakeToResetEnabled(true)
    runCurrent()

    assertThat(snapshot.remoteNextPrevSkipsChapters.value).isTrue()
    assertThat(snapshot.shakeToResetEnabled.value).isTrue()
  }
}
