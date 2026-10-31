// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

interface DownloadSettings {

  /**
   * Whether a podcast episode starts downloading as it starts playing or is added to the queue
   * (Up next).
   * Default: `false`
   */
  var downloadEpisodesOnPlayOrQueue: Boolean
  fun observeDownloadEpisodesOnPlayOrQueue(): StateFlow<Boolean>

  /**
   * Whether a podcast episode's download is removed once it plays to the end.
   * Default: `false`
   */
  var deleteFinishedEpisodes: Boolean
  fun observeDeleteFinishedEpisodes(): StateFlow<Boolean>
}
