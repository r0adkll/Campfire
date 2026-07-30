// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId

/**
 * Persisted alongside each `DownloadRequest` via `setData(...)`. Carries the routing
 * [key] plus user-facing display strings so the foreground service notification can
 * render an informative title without needing to hit the DB at update time.
 *
 * Wire format: four fields separated by ASCII Unit Separator (``) — picked
 * because it never appears in user-facing titles.
 *
 * Backward-compat: [decode] accepts three forms so already-shipped downloads keep
 * working without migration:
 *   - bare `<libraryItemId>` (pre-podcast book downloads)
 *   - `<libraryItemId>|<episodeId>` (the just-shipped episode format)
 *   - `<libraryItemId><episodeId><title><subtitle>` (this format)
 */
data class OfflineDownloadPayload(
  val key: OfflineDownloadKey,
  val title: String = "",
  val subtitle: String = "",
) {

  constructor(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
    title: String = "",
    subtitle: String = "",
  ) : this(OfflineDownloadKey(libraryItemId, episodeId), title, subtitle)

  fun encode(): ByteArray = buildString {
    append(key.libraryItemId)
    append(SEPARATOR)
    append(key.episodeId ?: "")
    append(SEPARATOR)
    append(title)
    append(SEPARATOR)
    append(subtitle)
  }.encodeToByteArray()

  companion object {
    private const val SEPARATOR = ''
    private const val LEGACY_SEPARATOR = '|'

    fun decode(bytes: ByteArray): OfflineDownloadPayload {
      val decoded = bytes.decodeToString()

      // New format: -separated fields.
      if (decoded.contains(SEPARATOR)) {
        val parts = decoded.split(SEPARATOR)
        return OfflineDownloadPayload(
          key = OfflineDownloadKey(
            libraryItemId = parts.getOrNull(0).orEmpty(),
            episodeId = parts.getOrNull(1)?.ifEmpty { null },
          ),
          title = parts.getOrNull(2).orEmpty(),
          subtitle = parts.getOrNull(3).orEmpty(),
        )
      }

      // Legacy episode format: <libraryItemId>|<episodeId>.
      if (decoded.contains(LEGACY_SEPARATOR)) {
        val parts = decoded.split(LEGACY_SEPARATOR, limit = 2)
        return OfflineDownloadPayload(
          key = OfflineDownloadKey(
            libraryItemId = parts[0],
            episodeId = parts.getOrNull(1)?.ifEmpty { null },
          ),
        )
      }

      // Legacy bare libraryItemId.
      return OfflineDownloadPayload(key = OfflineDownloadKey(libraryItemId = decoded))
    }
  }
}
