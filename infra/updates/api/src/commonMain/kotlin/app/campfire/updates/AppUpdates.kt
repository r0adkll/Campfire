// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import app.campfire.updates.source.AppUpdate
import app.campfire.updates.source.AppUpdateSheet
import kotlin.time.Instant
import kotlinx.coroutines.flow.StateFlow

/**
 * Where the app stands on updates, kept current by checks each time the app comes to the
 * foreground: what the drawer card, the Settings badges and the App updates pane all show.
 */
interface AppUpdates {

  /** Whether this build updates itself at all; nothing about updates shows when it doesn't. */
  val isSupported: Boolean

  /** The release channel this build follows, where the source has them. */
  val channel: String?

  /** The newest build waiting, with how far its install has got; null when up to date. */
  val update: StateFlow<AppUpdate?>

  /** [update], unless its drawer card was dismissed; an update under way can't be dismissed. */
  val undismissedUpdate: StateFlow<AppUpdate?>

  val check: StateFlow<AppUpdateCheck>

  /** The sheet that downloads and installs [update]: the source's own, or Campfire's. */
  val installSheet: AppUpdateSheet

  /** Check again now, regardless of the interval, e.g. after the update sheet closes. */
  fun refresh()

  /** Hides the drawer card for [update] until a different build is waiting. */
  fun dismiss(update: AppUpdate)
}

/** The last update check: whether one is running, when one last finished and if it failed. */
data class AppUpdateCheck(
  val isChecking: Boolean = false,
  val checkedAt: Instant? = null,
  val failed: Boolean = false,
)
