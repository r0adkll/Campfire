package app.campfire.audioplayer.offline

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId

/**
 * Composite key used to tag a download with the library item and (optionally) the
 * podcast episode it belongs to. Encoded into a [androidx.media3.exoplayer.offline.DownloadRequest]'s
 * `data` byte payload so the runtime download index can be grouped/observed per
 * `(libraryItemId, episodeId?)` pair.
 *
 * Encoding is `"<libraryItemId>|<episodeId>"`. The book path emits `episodeId = ""`
 * (so the key encodes to `"<libraryItemId>|"`). [decode] tolerates legacy book entries
 * that were written as a bare `libraryItemId` with no separator — those decode to
 * `episodeId = null`.
 */
data class OfflineDownloadKey(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
) {

  fun encode(): ByteArray = "$libraryItemId$SEPARATOR${episodeId ?: ""}".encodeToByteArray()

  companion object {
    private const val SEPARATOR = '|'

    fun decode(bytes: ByteArray): OfflineDownloadKey = OfflineDownloadPayload.decode(bytes).key
  }
}
