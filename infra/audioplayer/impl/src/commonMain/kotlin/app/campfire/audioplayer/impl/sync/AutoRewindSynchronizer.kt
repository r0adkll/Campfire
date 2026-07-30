// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PendingResumeRewind
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.rewindForPause
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * A [PlaybackSynchronizer] that rewinds playback when it resumes after having been paused, by an amount that
 * scales with how long playback was paused. The scaling is configured via [PlaybackSettings.resumeRewindConfig]
 * and gated by [PlaybackSettings.autoRewindOnResumeEnabled].
 *
 * We observe the shared [AudioPlayer.State] flow (via the synchronizer callbacks) rather than hooking each
 * resume entry point, so every resume source — in-app controls, media notification, Bluetooth, Android Auto,
 * lock screen, and system-initiated resumes after an audio interruption — is covered by one implementation.
 *
 * The pending-pause marker is **persisted** (see [PlaybackSettings.pendingResumeRewind]) rather than held in
 * memory, so a pause interrupted by the app being killed still rewinds when the same item resumes. The marker
 * is keyed on the library item, so starting a different item — or a fresh session — never triggers a spurious
 * rewind.
 *
 * [audioPlayerHolder] is injected lazily to break the dependency cycle between the holder (which owns the
 * synchronizers) and this synchronizer (which needs the holder's current player to issue the seek).
 */
@ContributesMultibinding(AppScope::class, boundType = PlaybackSynchronizer::class)
@Inject
class AutoRewindSynchronizer(
  private val playbackSettings: PlaybackSettings,
  private val audioPlayerHolder: Lazy<AudioPlayerHolder>,
  private val fatherTime: FatherTime,
  private val dispatcherProvider: DispatcherProvider,
) : PlaybackSynchronizer {

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    when (state) {
      // Only record a pause that interrupts active playback. A freshly prepared session that starts paused
      // (Initializing/Buffering -> Paused) should not later be treated as a resume-with-rewind.
      AudioPlayer.State.Paused -> {
        if (previousState == AudioPlayer.State.Playing && playbackSettings.autoRewindOnResumeEnabled) {
          playbackSettings.pendingResumeRewind = PendingResumeRewind(
            pausedAtEpochMillis = fatherTime.nowInEpochMillis(),
            libraryItemId = libraryItemId,
          )
        }
      }

      // Resuming — rewind if there is a pending pause for THIS item, then consume the marker.
      AudioPlayer.State.Playing -> {
        val pending = playbackSettings.pendingResumeRewind ?: return
        if (pending.libraryItemId != libraryItemId) return
        playbackSettings.pendingResumeRewind = null
        rewindOnResume(pending.pausedAtEpochMillis)
      }

      // Other transitions (Buffering between pause and resume, session boundaries) leave the persisted marker
      // untouched; it is only consumed by a matching resume and only overwritten by a newer pause.
      AudioPlayer.State.Disabled,
      AudioPlayer.State.Initializing,
      AudioPlayer.State.Buffering,
      AudioPlayer.State.Finished,
      -> Unit
    }
  }

  private suspend fun rewindOnResume(pausedAtEpochMillis: Long) {
    if (!playbackSettings.autoRewindOnResumeEnabled) return
    val player = audioPlayerHolder.value.currentPlayer.value ?: return

    val pauseDuration = (fatherTime.nowInEpochMillis() - pausedAtEpochMillis).milliseconds
    var rewindAmount = playbackSettings.resumeRewindConfig.rewindForPause(pauseDuration)
    if (rewindAmount <= Duration.ZERO) return

    // Optionally keep the rewind within the current chapter. Chapters are the player's media items, so
    // [AudioPlayer.currentTime] is the position within the current chapter — capping the rewind at it stops
    // playback from crossing back into the previous chapter.
    if (playbackSettings.autoRewindStopAtChapterBoundary) {
      rewindAmount = rewindAmount.coerceAtMost(player.currentTime.value)
    }
    if (rewindAmount <= Duration.ZERO) return

    val target = (player.overallTime.value - rewindAmount).coerceAtLeast(Duration.ZERO)
    dbark { "Resumed after $pauseDuration paused; rewinding $rewindAmount to $target" }

    withContext(dispatcherProvider.main) {
      player.seekTo(target)
    }
  }

  companion object : Cork {
    override val tag: String = "AutoRewindSynchronizer"
    override val enabled: Boolean = false
  }
}
