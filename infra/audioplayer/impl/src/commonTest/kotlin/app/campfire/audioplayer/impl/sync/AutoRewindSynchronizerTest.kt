// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:OptIn(ExperimentalCoroutinesApi::class)

package app.campfire.audioplayer.impl.sync

import app.campfire.audioplayer.AudioPlayer.State
import app.campfire.audioplayer.test.FakeAudioPlayer
import app.campfire.audioplayer.test.FakeAudioPlayer.Invocation
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.ResumeRewindConfig
import app.campfire.settings.test.FakePlaybackSettings
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class AutoRewindSynchronizerTest {

  // With this config the tiers are [5s→10s, 1m→20s, 5m→30s, 15m→40s, 30m→50s]
  private val cleanConfig = ResumeRewindConfig(
    minPauseThreshold = 5.seconds,
    minRewind = 10.seconds,
    maxRewind = 50.seconds,
  )

  // Unconfined so the seek issued via withContext(main) runs eagerly and is observable synchronously.
  private val testDispatcher = UnconfinedTestDispatcher()
  private val dispatcherProvider = DispatcherProvider(
    io = testDispatcher,
    databaseWrite = testDispatcher,
    databaseRead = testDispatcher,
    computation = testDispatcher,
    main = testDispatcher,
  )

  private val fatherTime = MutableFatherTime()
  private val settings = FakePlaybackSettings().apply {
    autoRewindOnResumeEnabled = true
    resumeRewindConfig = cleanConfig
    // Most tests exercise the raw rewind; chapter-boundary clamping has its own dedicated tests below.
    autoRewindStopAtChapterBoundary = false
  }
  private val player = FakeAudioPlayer()
  private val holder = FakeAudioPlayerHolder().apply { setCurrentPlayer(player) }

  private fun synchronizer() = AutoRewindSynchronizer(
    playbackSettings = settings,
    audioPlayerHolder = lazy { holder },
    fatherTime = fatherTime,
    dispatcherProvider = dispatcherProvider,
  )

  @Test
  fun `resuming the same item after a long pause rewinds by the matching tier`() = runTest(testDispatcher) {
    val sync = synchronizer()
    player.overallTime.value = 100.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds // >= 5m tier -> 30s
    sync.resume(ITEM_A)

    assertThat(player.lastSeek()).isEqualTo(70.seconds)
    assertThat(settings.pendingResumeRewind).isNull() // consumed
  }

  @Test
  fun `pause shorter than the floor does not rewind`() = runTest(testDispatcher) {
    val sync = synchronizer()
    player.overallTime.value = 100.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 3.seconds.inWholeMilliseconds // below 5s floor
    sync.resume(ITEM_A)

    assertThat(player.didSeek()).isFalse()
  }

  @Test
  fun `disabled setting never records or rewinds`() = runTest(testDispatcher) {
    settings.autoRewindOnResumeEnabled = false
    val sync = synchronizer()
    player.overallTime.value = 100.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    assertThat(settings.pendingResumeRewind).isNull() // never recorded
    fatherTime.nowMillis = 10.minutes.inWholeMilliseconds
    sync.resume(ITEM_A)

    assertThat(player.didSeek()).isFalse()
  }

  @Test
  fun `first play with no pending pause does not rewind`() = runTest(testDispatcher) {
    val sync = synchronizer()
    player.overallTime.value = 100.seconds

    // Fresh session: Initializing -> Paused (not from Playing) then Playing
    fatherTime.nowMillis = 0
    sync.onStateChanged(SESSION, ITEM_A, State.Paused, State.Initializing)
    fatherTime.nowMillis = 10.minutes.inWholeMilliseconds
    sync.resume(ITEM_A)

    assertThat(player.didSeek()).isFalse()
  }

  @Test
  fun `resuming a different item does not rewind and preserves the pending pause`() = runTest(testDispatcher) {
    val sync = synchronizer()
    player.overallTime.value = 100.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds
    sync.resume(ITEM_B)

    assertThat(player.didSeek()).isFalse()
    // A's pending pause is untouched, ready for when A resumes
    assertThat(settings.pendingResumeRewind).isEqualTo(PendingResumeRewind(0, ITEM_A))
  }

  @Test
  fun `a pause that survives process death still rewinds on resume`() = runTest(testDispatcher) {
    // Simulate: paused, then the app was killed. The pending marker was persisted.
    settings.pendingResumeRewind = PendingResumeRewind(pausedAtEpochMillis = 0, libraryItemId = ITEM_A)
    player.overallTime.value = 100.seconds

    // A brand-new synchronizer instance (no in-memory state) after relaunch
    val sync = synchronizer()
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds
    sync.resume(ITEM_A)

    assertThat(player.lastSeek()).isEqualTo(70.seconds)
    assertThat(settings.pendingResumeRewind).isNull()
  }

  @Test
  fun `chapter boundary clamp stops the rewind at the current chapter start`() = runTest(testDispatcher) {
    settings.autoRewindStopAtChapterBoundary = true
    val sync = synchronizer()
    player.overallTime.value = 100.seconds
    player.currentTime.value = 10.seconds // only 10s into the current chapter

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds // 30s rewind, but clamp to 10s
    sync.resume(ITEM_A)

    assertThat(player.lastSeek()).isEqualTo(90.seconds) // 100s - 10s (chapter start), not 70s
  }

  @Test
  fun `chapter boundary clamp does nothing at the very start of a chapter`() = runTest(testDispatcher) {
    settings.autoRewindStopAtChapterBoundary = true
    val sync = synchronizer()
    player.overallTime.value = 100.seconds
    player.currentTime.value = Duration.ZERO // exactly at a chapter boundary

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds
    sync.resume(ITEM_A)

    assertThat(player.didSeek()).isFalse()
  }

  @Test
  fun `with the clamp off the rewind crosses the chapter boundary`() = runTest(testDispatcher) {
    settings.autoRewindStopAtChapterBoundary = false
    val sync = synchronizer()
    player.overallTime.value = 100.seconds
    player.currentTime.value = 10.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds
    sync.resume(ITEM_A)

    assertThat(player.lastSeek()).isEqualTo(70.seconds) // full 30s rewind, ignoring the boundary
  }

  @Test
  fun `rewind is clamped to zero`() = runTest(testDispatcher) {
    val sync = synchronizer()
    player.overallTime.value = 3.seconds

    fatherTime.nowMillis = 0
    sync.pause(ITEM_A)
    fatherTime.nowMillis = 6.minutes.inWholeMilliseconds // 30s rewind, only 3s exists
    sync.resume(ITEM_A)

    assertThat(player.lastSeek()).isEqualTo(Duration.ZERO)
  }

  private suspend fun AutoRewindSynchronizer.pause(item: LibraryItemId) =
    onStateChanged(SESSION, item, State.Paused, State.Playing)

  private suspend fun AutoRewindSynchronizer.resume(item: LibraryItemId) =
    onStateChanged(SESSION, item, State.Playing, State.Paused)

  private fun FakeAudioPlayer.didSeek(): Boolean = invocations.any { it is Invocation.SeekTo }

  private fun FakeAudioPlayer.lastSeek(): Any? =
    invocations.filterIsInstance<Invocation.SeekTo>().lastOrNull()?.value

  companion object {
    private val SESSION = Uuid.fromLongs(0L, 0L)
    private const val ITEM_A: LibraryItemId = "item_a"
    private const val ITEM_B: LibraryItemId = "item_b"
  }
}

private class MutableFatherTime(var nowMillis: Long = 0L) : FatherTime {
  override fun now(): LocalDateTime = error("not used in tests")
  override fun today(): LocalDate = error("not used in tests")
  override fun nowInEpochMillis(): Long = nowMillis
}
