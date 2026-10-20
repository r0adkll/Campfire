// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

/** A release channel as testers know it, read from the update source's channel name. */
sealed interface ReleaseChannel {

  /** Every merge to main: the alpha builds. */
  data object Main : ReleaseChannel

  data object Beta : ReleaseChannel

  /** A pull request's own builds, which end when it closes. */
  data class PullRequest(val number: Int) : ReleaseChannel

  data class Other(val name: String) : ReleaseChannel

  companion object {
    fun parse(channel: String): ReleaseChannel = when {
      channel == "main" -> Main
      channel == "beta" -> Beta
      channel.startsWith("pr/") -> channel.removePrefix("pr/").toIntOrNull()?.let(::PullRequest) ?: Other(channel)
      else -> Other(channel)
    }
  }
}
