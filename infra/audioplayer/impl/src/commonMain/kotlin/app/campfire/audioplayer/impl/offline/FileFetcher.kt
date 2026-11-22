// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import kotlinx.io.IOException
import kotlinx.io.files.Path

/**
 * Streams one remote file into a `.part` file, resuming from whatever a previous attempt left
 * behind via an HTTP `Range` request.
 */
internal fun interface FileFetcher {

  /**
   * Download [request] into [partFile], appending to its current contents when the server honors
   * a range request. Returns once the full body is on disk; throws on failure or cancellation,
   * leaving [partFile] in place for a later resume. An [IOException] is retried by the queue, a
   * [PermanentFetchException] is not.
   *
   * @param onProgress called with (bytes on disk, total bytes or <= 0 when unknown)
   */
  suspend fun fetch(request: FetchRequest, partFile: Path, onProgress: (Long, Long) -> Unit)
}

internal data class FetchRequest(
  val url: String,
  val userId: String,
  val serverUrl: String,
  val expectedBytes: Long,
)

/** A failure that retrying won't fix — a rejected credential, a missing file, a denied permission. */
internal class PermanentFetchException(message: String) : IOException(message)
