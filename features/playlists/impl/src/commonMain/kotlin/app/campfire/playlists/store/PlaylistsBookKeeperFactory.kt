package app.campfire.playlists.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.userId
import app.campfire.data.PlaylistsBookkeeping
import app.campfire.playlists.store.PlaylistsStore.Operation
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.Bookkeeper

class PlaylistsBookKeeperFactory(
  private val userSessionManager: UserSessionManager,
  private val db: CampfireDatabase,
  private val dispatcherProvider: DispatcherProvider,
) {

  fun create(): Bookkeeper<Operation> {
    return Bookkeeper.by(
      getLastFailedSync = { operation -> getLastFailedSync(operation) },
      setLastFailedSync = { operation, time ->
        require(operation is Operation.Mutation)
        setFailedSync(operation, time)
        true
      },
      clear = { operation -> clear(operation) },
      clearAll = {
        val currentUserId = userSessionManager.current.userId ?: return@by false
        withContext(dispatcherProvider.databaseWrite) {
          db.playlistsBookkeepingQueries.deleteAll(currentUserId)
        }
        true
      },
    )
  }

  private suspend fun getLastFailedSync(
    operation: Operation,
  ): Long? = withContext(dispatcherProvider.databaseRead) {
    when (operation) {
      is Operation.All -> {
        db.playlistsBookkeepingQueries
          .getLastFailedSyncs(operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }

      is Operation.Single -> {
        db.playlistsBookkeepingQueries
          .getLastFailedSync(operation.playlistId, operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }

      is Operation.Mutation -> {
        db.playlistsBookkeepingQueries
          .getLastFailedSyncForOperation(operation.key, operation.userId)
          .executeAsOneOrNull()
          ?.MAX
      }
    }
  }

  private suspend fun setFailedSync(
    mutation: Operation.Mutation,
    time: Long,
  ) = withContext(dispatcherProvider.databaseWrite) {
    when (mutation) {
      is Operation.Mutation.Delete -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.id,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Update -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.id,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Create -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.creationId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Add -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.playlistId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.Remove -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.playlistId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }

      is Operation.Mutation.FromCollection -> {
        db.playlistsBookkeepingQueries
          .insertFailedSync(
            PlaylistsBookkeeping(
              userId = mutation.userId,
              playlistId = mutation.collectionId,
              operation = mutation.key,
              timestamp = time,
            ),
          )
      }
    }
  }

  private suspend fun clear(
    operation: Operation,
  ) = withContext(dispatcherProvider.databaseWrite) {
    when (operation) {
      is Operation.All -> {
        db.playlistsBookkeepingQueries
          .deleteAll(operation.userId)

        true
      }

      is Operation.Single -> {
        db.playlistsBookkeepingQueries
          .deleteFor(operation.playlistId, operation.userId)

        true
      }

      else -> false
    }
  }
}
