// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.logging.Corked
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.sessions.api.SessionsRepository
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import com.r0adkll.kimchi.annotations.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@ContributesTo(UserScope::class)
interface LocalSessionComponent {
  val sessionsRepository: SessionsRepository
  val remoteSessionsUpdater: RemoteSessionsUpdater
}

@Inject
@SingleIn(AppScope::class)
@ContributesMultibinding(AppScope::class)
class LocalSessionUpdateSynchronizer(
  private val fatherTime: FatherTime,
) : PlaybackSynchronizer {

  // FIXME: This is a hack to get around the AppScope -> UserScope issue.
  //  We need to find a better way to organize our user management and scoping to prevent this
  //  when we DON'T really need the separation for some of these operations
  private val component: LocalSessionComponent
    get() = ComponentHolder.component<LocalSessionComponent>()

  private var lastPlayedTime: Long? = null

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    ibark { "onStateChanged: $state from $previousState" }
    if (state == AudioPlayer.State.Playing) {
      lastPlayedTime = fatherTime.nowInEpochMillis()
      ibark { "Setting lastPlayedTime to $lastPlayedTime" }
    } else if (
      state == AudioPlayer.State.Paused ||
      state == AudioPlayer.State.Disabled ||
      state == AudioPlayer.State.Finished
    ) {
      if (lastPlayedTime != null) {
        val elapsed = (fatherTime.nowInEpochMillis() - lastPlayedTime!!).milliseconds
        ibark { "Adding $elapsed time listening for ${sessionId.toHexDashString()})" }
        component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
        component.remoteSessionsUpdater.update(skipInterval = true)
        lastPlayedTime = null
      }
    }

    // Update the sessions last "Played" time. This way we can keep track of when
    // the local session was last "Played" to compare against the current MediaProgress
    // from the backend. If the progress is newer than the last played time on the device
    // then the user might want to sync their progress, or enable auto-sync on new-sessions
    if (
      state == AudioPlayer.State.Playing ||
      (state == AudioPlayer.State.Paused && previousState == AudioPlayer.State.Playing)
    ) {
      component.sessionsRepository.updateLastPlayed(libraryItemId)
    }
  }

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    component.sessionsRepository.updateCurrentTime(libraryItemId, overallTime)

    // Check if its been too long since we synced listening time
    if (lastPlayedTime != null) {
      val elapsed = (fatherTime.nowInEpochMillis() - lastPlayedTime!!).milliseconds
      if (elapsed > MAX_TIME_LISTENING_INTERVAL) {
        ibark { "Timeout adding $elapsed time listening)" }
        component.sessionsRepository.addTimeListening(libraryItemId, elapsed)
        lastPlayedTime = fatherTime.nowInEpochMillis()
      }
    }

    // Periodic sync while playing (throttled to 15s/60s-metered inside the updater).
    // This was disabled in #682 out of caution for multi-device sync, but the protection
    // was never write-avoidance: the MediaProgress source-of-truth freshness guard is what
    // keeps a device's own server echoes from clobbering fresher local state, and the
    // progress PATCH path pushed on this same cadence all along.
    component.remoteSessionsUpdater.update()
  }

  companion object : Corked("LocalSessionUpdateSynchronizer") {
    private val MAX_TIME_LISTENING_INTERVAL = 1.minutes
  }
}
