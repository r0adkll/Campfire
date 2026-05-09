package app.campfire.playlists.store

import app.campfire.CampfireDatabase
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.model.UserId
import app.campfire.data.PlaylistItemJoin
import app.campfire.data.mapping.asDbModel
import app.campfire.network.AudioBookShelfApi
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.models.PlaylistItem
import app.campfire.playlists.store.PlaylistsStore.Operation
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import org.mobilenativefoundation.store.store5.OnUpdaterCompletion
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

class PlaylistsUpdaterFactory(
  private val api: AudioBookShelfApi,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): Updater<Operation, PlaylistsStore.Output, PlaylistsStore.Update> {
    return Updater.by(
      post = { operation, output ->
        require(operation is Operation.Mutation)
        when (operation) {
          is Operation.Mutation.Create -> handleCreate(operation)
          is Operation.Mutation.Update -> handleUpdate(operation)
          is Operation.Mutation.Add -> handleAdd(operation)
          is Operation.Mutation.Remove -> handleRemove(operation)
          is Operation.Mutation.Delete -> handleDelete(operation)
          is Operation.Mutation.FromCollection -> handleFromCollection(operation)
        }
      },
      onCompletion = OnUpdaterCompletion(
        onSuccess = { result ->
        },
        onFailure = {
        },
      ),
    )
  }

  private suspend fun handleCreate(mutation: Operation.Mutation.Create): UpdaterResult {
    val result = api.createPlaylist(
      libraryId = mutation.libraryId,
      name = mutation.name,
      description = mutation.description,
      items = mutation.items.map { item ->
        PlaylistItem.Minified(
          libraryItemId = item.libraryItemId,
          episodeId = item.episodeId,
        )
      },
    )

    return if (result.isSuccess) {
      updateLocalCreate(mutation.userId, mutation.libraryId, mutation.creationId, result.getOrThrow())
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to create the playlist")
    }
  }

  private suspend fun updateLocalCreate(
    userId: UserId,
    libraryId: LibraryId,
    creationId: String,
    playlist: PlaylistExpanded,
  ) {
    val existing = db.playlistsQueries
      .selectById(creationId)
      .awaitAsOneOrNull()

    if (existing != null) {
      db.transaction {
        // Insert a copy, with the real id, while maintaining the creation id
        // for any observers that need it.
        db.playlistsQueries.insert(playlist.asDbModel(userId, libraryId, creationId))

        // Copy over the junction entries
        playlist.items.forEachIndexed { index, item ->
          db.playlistItemJoinQueries.insert(
            PlaylistItemJoin(playlist.id, item.libraryItemId, item.episodeId.orEmpty(), index),
          )
        }

        // Delete the old stuff
        db.playlistsQueries.delete(creationId)
        db.playlistItemJoinQueries.delete(creationId)
      }
    }
  }

  private suspend fun handleUpdate(mutation: Operation.Mutation.Update): UpdaterResult {
    val result = api.updatePlaylist(
      playlistId = mutation.id,
      name = mutation.name,
      description = mutation.description,
      items = mutation.items.map { item ->
        PlaylistItem.Minified(
          libraryItemId = item.libraryItemId,
          episodeId = item.episodeId,
        )
      },
    )

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to update the playlist")
    }
  }

  private suspend fun handleAdd(mutation: Operation.Mutation.Add): UpdaterResult {
    val result = api.addToPlaylist(
      playlistId = mutation.playlistId,
      item = PlaylistItem.Minified(
        libraryItemId = mutation.item.libraryItemId,
        episodeId = mutation.item.episodeId,
      ),
    )

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to add the item to the playlist")
    }
  }

  private suspend fun handleRemove(mutation: Operation.Mutation.Remove): UpdaterResult {
    val result = api.removeFromPlaylist(
      playlistId = mutation.playlistId,
      item = PlaylistItem.Minified(
        libraryItemId = mutation.item.libraryItemId,
        episodeId = mutation.item.episodeId,
      ),
    )

    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to remove the item from the playlist")
    }
  }

  private suspend fun handleDelete(mutation: Operation.Mutation.Delete): UpdaterResult {
    val result = api.deletePlaylist(mutation.id)
    return if (result.isSuccess) {
      UpdaterResult.Success.Untyped(Unit)
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to delete the playlist")
    }
  }

  private suspend fun handleFromCollection(mutation: Operation.Mutation.FromCollection): UpdaterResult {
    val result = api.createPlaylistFromCollection(mutation.collectionId)
    return if (result.isSuccess) {
      UpdaterResult.Success.Typed(result.getOrThrow())
    } else {
      result.exceptionOrNull()?.let { UpdaterResult.Error.Exception(it) }
        ?: UpdaterResult.Error.Message("Unable to create playlist from collection")
    }
  }
}
