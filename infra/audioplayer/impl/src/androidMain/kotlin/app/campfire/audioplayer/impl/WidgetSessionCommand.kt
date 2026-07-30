// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

/**
 * Custom [SessionCommand] action strings used to send app-specific commands
 * from a [MediaController] (e.g. widget) to the [AudioPlayerService].
 */
object WidgetSessionCommand {
  const val CYCLE_SPEED = "app.campfire.action.CYCLE_PLAYBACK_SPEED"
  const val SET_SLEEP_TIMER = "app.campfire.action.SET_SLEEP_TIMER"
  const val CLEAR_SLEEP_TIMER = "app.campfire.action.CLEAR_SLEEP_TIMER"

  const val ARG_TIMER_MINUTES = "minutes"
}
