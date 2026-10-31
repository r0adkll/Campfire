// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.user.api

/**
 * Why [MediaProgressRepository.setStartedAt] didn't change when a progress started. Audiobookshelf
 * only sets the start when it creates a progress, so changing it deletes the progress and creates
 * it again, and each of these says how far that got.
 */
sealed class StartedAtNotChanged(message: String, cause: Throwable? = null) : Exception(message, cause) {

  /** Nothing changed on the server: it couldn't be reached, or refused before the progress was deleted. */
  class Unchanged(cause: Throwable? = null) : StartedAtNotChanged("The progress was left as it was", cause)

  /** The progress was put back as it was after creating it again failed. */
  class Restored(cause: Throwable? = null) : StartedAtNotChanged("The progress was put back as it was", cause)

  /**
   * The server kept a start of its own, most likely because another device synced the progress
   * between it being deleted and created again. The progress is otherwise as it was.
   */
  class ServerKept : StartedAtNotChanged("The server kept its own start date")

  /**
   * The progress couldn't be put back on the server. This device still has it, and
   * [MediaProgressRepository.restoreProgress] tries again.
   */
  class NotRestored(cause: Throwable? = null) : StartedAtNotChanged("The progress couldn't be put back", cause)
}
