package app.campfire.sharing

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaShare
import app.campfire.core.model.MediaShareId
import kotlinx.coroutines.flow.Flow

interface SharingRepository {

  suspend fun createMediaShare(
    slug: String,
    libraryItem: LibraryItem,
    expiresAtEpochMs: Long,
    isDownloadable: Boolean,
  ): Result<MediaShare>

  fun observeAllShares(): Flow<List<MediaShare>>

  suspend fun deleteShare(shareId: MediaShareId): Result<Unit>
}
