// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.paging

import app.campfire.data.SelectUnfinishedEpisodes

/**
 * How many of a podcast's episodes the user hasn't finished: the server's count when the podcast's
 * library page loaded, less the episodes finished on this device since, plus the ones that were
 * finished then and aren't anymore.
 *
 * A finished episode whose progress only reaches this device after the page loaded, such as one
 * finished elsewhere while the app was closed, keeps its earlier finish time. The server had
 * already counted it, so it isn't taken off again.
 */
internal fun SelectUnfinishedEpisodes.unfinishedEpisodes(): Int {
  val unfinishedSinceLoad = ((numEpisodesFinishedAtLoad ?: 0) - finishedBeforeLoad).coerceAtLeast(0)
  return (numEpisodesIncomplete - finishedSinceLoad + unfinishedSinceLoad)
    .coerceAtLeast(0)
    .toInt()
}
