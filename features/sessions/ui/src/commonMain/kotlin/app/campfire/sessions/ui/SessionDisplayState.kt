// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import app.campfire.audioplayer.model.Metadata
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Session
import kotlin.time.Duration

/**
 * The timing and metadata the playback surfaces should display for a session while no audio
 * player is prepared for it yet — reopening the app after the media service died, or before
 * the resume-prime pipeline (service start, session creation, routing, prepare) completes.
 *
 * Derivation mirrors the player's own prepare-time seeding exactly, so when the player comes
 * alive its flows take over with identical values and the handoff is invisible: episodes show
 * episode-relative time against the episode duration; books show chapter-relative time when a
 * chapter table exists, track-relative as the fallback, and absolute time otherwise.
 */
data class SessionDisplayState(
  val time: Duration,
  val bookTime: Duration,
  val duration: Duration,
  val metadata: Metadata,
)

fun Session.placeholderDisplayState(): SessionDisplayState {
  // The chapter/track accessors derive from currentTime, so an invalid stored time must be
  // normalized on the session itself — not just the returned values
  val normalized = if (currentTime.isFinite() && currentTime >= Duration.ZERO) {
    this
  } else {
    copy(currentTime = Duration.ZERO)
  }
  return normalized.derivePlaceholder()
}

private fun Session.derivePlaceholder(): SessionDisplayState {
  val artwork = libraryItem.media.coverImageUrl
  val safeTime = currentTime

  val episode = episode
  if (episode != null) {
    return SessionDisplayState(
      time = safeTime,
      bookTime = safeTime,
      duration = episode.duration,
      metadata = Metadata(title = episode.title, artworkUri = artwork),
    )
  }

  val chapter = chapter
  if (chapter != null) {
    return SessionDisplayState(
      time = (safeTime - chapter.start.seconds).coerceAtLeast(Duration.ZERO),
      bookTime = safeTime,
      duration = chapter.duration,
      metadata = Metadata(title = chapter.title, artworkUri = artwork),
    )
  }

  val track = audioTrack
  if (track != null) {
    return SessionDisplayState(
      time = (safeTime - track.startOffset.seconds).coerceAtLeast(Duration.ZERO),
      bookTime = safeTime,
      duration = track.duration.seconds,
      metadata = Metadata(title = track.taggedTitle, artworkUri = artwork),
    )
  }

  return SessionDisplayState(
    time = safeTime,
    bookTime = safeTime,
    duration = duration,
    metadata = Metadata(title = libraryItem.media.metadata.title, artworkUri = artwork),
  )
}
