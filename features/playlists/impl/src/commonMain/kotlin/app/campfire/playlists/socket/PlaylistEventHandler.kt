package app.campfire.playlists.socket

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.model.PlaylistId
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import app.campfire.data.mapping.asDbModel
import app.campfire.network.models.PlaylistExpanded
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

/**
 * Handles `Playlist*` socket events by writing through to the local DB.
 *
 * Only the `playlists` row is mirrored — the join table (`playlistItemJoin`) is repopulated on
 * the next fetch, matching how `StorePlaylistsRepository` writes its SoT.
 *
 * The wire `Playlist` model does NOT carry `libraryId` directly. For the `Expanded` variant we
 * peek the first item's nested `libraryItem.libraryId`. For updates against an empty playlist we
 * fall back to looking up the existing row in the DB. Add events for empty playlists are
 * skipped — they'll get hydrated on the next list fetch.
 */
interface PlaylistEventHandler {
  suspend fun onPlaylistAdded(playlist: PlaylistExpanded)
  suspend fun onPlaylistUpdated(playlist: PlaylistExpanded)
  suspend fun onPlaylistRemoved(playlistId: PlaylistId)
}

@ContributesBinding(UserScope::class)
@Inject
class DefaultPlaylistEventHandler(
  private val db: CampfireDatabase,
  private val userSession: UserSession,
  private val dispatcherProvider: DispatcherProvider,
) : PlaylistEventHandler {

  override suspend fun onPlaylistAdded(playlist: PlaylistExpanded) = upsert(playlist)

  override suspend fun onPlaylistUpdated(playlist: PlaylistExpanded) = upsert(playlist)

  override suspend fun onPlaylistRemoved(playlistId: PlaylistId) {
    withContext(dispatcherProvider.databaseWrite) {
      db.playlistsQueries.delete(playlistId)
    }
  }

  private suspend fun upsert(playlist: PlaylistExpanded) {
    val userId = userSession.userId ?: return
    val libraryId = playlist.libraryIdOrLookup() ?: return
    val dbModel = playlist.asDbModel(userId = userId, libraryId = libraryId)
    withContext(dispatcherProvider.databaseWrite) {
      db.playlistsQueries.insert(dbModel)
    }
  }

  private suspend fun PlaylistExpanded.libraryIdOrLookup(): String? {
    items.firstOrNull()?.libraryItem?.libraryId?.let { return it }
    return withContext(dispatcherProvider.databaseRead) {
      db.playlistsQueries.selectById(id).executeAsOneOrNull()?.libraryId
    }
  }
}
