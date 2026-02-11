package app.campfire.playlists.store

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.model.LibraryId
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.core.model.UserId
import app.campfire.core.time.FatherTime
import app.campfire.data.PlaylistItemJoin
import app.campfire.data.Playlists
import app.campfire.data.mapping.asDbModel
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import org.mobilenativefoundation.store.store5.SourceOfTruth

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsSourceOfTruthFactory(
  private val db: CampfireDatabase,
  private val libraryItemDao: LibraryItemDao,
  private val dispatcherProvider: DispatcherProvider,
  private val urlHydrator: UrlHydrator,
  private val fatherTime: FatherTime,
) {

  fun create(): SourceOfTruth<PlaylistsStore.Operation, List<Playlist>, PlaylistsStore.Output> {
    return SourceOfTruth.of(
      reader = { operation -> handleRead(operation) },
      writer = { operation, playlists -> handleWrite(operation, playlists) },
      delete = { operation ->
        require(operation is PlaylistsStore.Operation.Mutation.Delete)
        handleDelete(operation)
      },
    )
  }

  private fun handleRead(operation: PlaylistsStore.Operation): Flow<PlaylistsStore.Output> {
    require(operation is PlaylistsStore.Operation.All || operation is PlaylistsStore.Operation.Single)
    return when (operation) {
      is PlaylistsStore.Operation.All -> readAll(operation.userId, operation.libraryId)
      is PlaylistsStore.Operation.Single -> readSingle(operation.playlistId, operation.isCreatedId)
      else -> throw IllegalArgumentException("Unknown operation: $operation")
    }
  }

  private fun readAll(userId: UserId, libraryId: LibraryId): Flow<PlaylistsStore.Output.Collection> {
    return db.playlistsQueries.selectByLibraryId(libraryId, userId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { playlists ->
        playlists.map { playlist ->
          val items = hydratePlaylistItems(playlist.id)
          playlist.asDomainModel(items)
        }
      }
      .map { PlaylistsStore.Output.Collection(it) }
  }

  private fun readSingle(playlistId: PlaylistId, isCreatedId: Boolean): Flow<PlaylistsStore.Output.Single> {
    return if (isCreatedId) {
      db.playlistsQueries.selectByCreatedId(playlistId)
    } else {
      db.playlistsQueries.selectById(playlistId)
    }
      .asFlow()
      .mapToOneOrNull(dispatcherProvider.databaseRead)
      .mapNotNull { playlist ->
        playlist?.let { p ->
          val items = hydratePlaylistItems(p.id)
          p.asDomainModel(items)
        }
      }
      .map { PlaylistsStore.Output.Single(it) }
  }

  private suspend fun hydratePlaylistItems(playlistId: PlaylistId): List<Playlist.Item.Expanded> {
    // Get junction entries to access episodeId and ordering
    val junctionEntries = db.playlistItemJoinQueries
      .selectForPlaylist(playlistId)
      .awaitAsList()

    // Get the hydrated library items
    val libraryItems = db.libraryItemsQueries
      .selectForPlaylist(playlistId)
      .awaitAsList()
      .map { it.asDomainModel(urlHydrator) }

    // Map library items by id for efficient lookup
    val itemsById = libraryItems.associateBy { it.id }

    // Combine junction data with library items
    return junctionEntries.mapIndexedNotNull { index, junction ->
      val libraryItem = itemsById[junction.libraryItemId] ?: return@mapIndexedNotNull null
      Playlist.Item.Expanded(
        index = index,
        libraryItemId = junction.libraryItemId,
        episodeId = junction.episodeId,
        libraryItem = libraryItem,
      )
    }
  }

  private suspend fun handleWrite(
    operation: PlaylistsStore.Operation,
    playlists: List<Playlist> = emptyList(),
  ) {
    when (operation) {
      is PlaylistsStore.Operation.All -> writeAll(operation.userId, operation.libraryId, playlists)
      is PlaylistsStore.Operation.Single -> writeSingle(operation.userId, operation.libraryId, playlists.first())
      is PlaylistsStore.Operation.Mutation.Create -> writeCreate(operation)
      is PlaylistsStore.Operation.Mutation.Update -> writeUpdate(operation)
      is PlaylistsStore.Operation.Mutation.Add -> handleAdd(operation)
      is PlaylistsStore.Operation.Mutation.Remove -> handleRemove(operation)
      is PlaylistsStore.Operation.Mutation.Delete -> handleDelete(operation)
      is PlaylistsStore.Operation.Mutation.FromCollection -> {
        // No-op for SoT write; the updater handles persisting the server response
      }
    }
  }

  private suspend fun writeAll(
    userId: UserId,
    libraryId: LibraryId,
    playlists: List<Playlist>,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      // Delete any existing entries that no longer exist
      db.playlistsQueries.deleteOld(playlists.map { it.id })

      playlists.forEach { playlist ->
        // Insert playlist
        db.playlistsQueries.insert(playlist.asDbModel(userId, libraryId))

        // Insert the playlist items
        playlist.items.forEachIndexed { index, item ->

          // These items contain expanded library items, so they should be safe to insert/replace
          libraryItemDao.insert(
            item = item.libraryItem,
            asTransaction = false,
          )

          // Insert junction entry
          db.playlistItemJoinQueries.insert(
            PlaylistItemJoin(
              playlistId = playlist.id,
              libraryItemId = item.libraryItemId,
              episodeId = item.episodeId,
              itemOrder = index,
            ),
          )
        }
      }
    }
  }

  internal suspend fun writeSingle(
    userId: UserId,
    libraryId: LibraryId,
    playlist: Playlist,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      // Insert playlist
      db.playlistsQueries.insert(playlist.asDbModel(userId, libraryId))

      // Insert the playlist items
      playlist.items.forEachIndexed { index, item ->

        // These items contain expanded library items, so they should be safe to insert/replace
        libraryItemDao.insert(
          item = item.libraryItem,
          asTransaction = false,
        )

        // Insert junction entry
        db.playlistItemJoinQueries.insert(
          PlaylistItemJoin(
            playlistId = playlist.id,
            libraryItemId = item.libraryItemId,
            episodeId = item.episodeId,
            itemOrder = index,
          ),
        )
      }
    }
  }

  private suspend fun writeCreate(
    mutation: PlaylistsStore.Operation.Mutation.Create,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.playlistsQueries
        .insert(
          Playlists(
            id = mutation.creationId,
            creationId = mutation.creationId,
            userId = mutation.userId,
            name = mutation.name,
            description = mutation.description,
            libraryId = mutation.libraryId,
            lastUpdated = fatherTime.nowInEpochMillis(),
            createdAt = fatherTime.nowInEpochMillis(),
          ),
        )

      mutation.items.forEachIndexed { index, item ->
        db.playlistItemJoinQueries.insert(
          PlaylistItemJoin(
            playlistId = mutation.creationId,
            libraryItemId = item.libraryItemId,
            episodeId = item.episodeId,
            itemOrder = index,
          ),
        )
      }
    }
  }

  private suspend fun writeUpdate(
    mutation: PlaylistsStore.Operation.Mutation.Update,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.playlistsQueries.updateName(
        name = mutation.name,
        lastUpdated = fatherTime.nowInEpochMillis(),
        id = mutation.id,
      )
      if (mutation.description != null) {
        db.playlistsQueries.updateDescription(
          description = mutation.description,
          lastUpdated = fatherTime.nowInEpochMillis(),
          id = mutation.id,
        )
      }

      // Replace all junction entries with the updated items list
      db.playlistItemJoinQueries.delete(mutation.id)
      mutation.items.forEachIndexed { index, item ->
        db.playlistItemJoinQueries.insert(
          PlaylistItemJoin(
            playlistId = mutation.id,
            libraryItemId = item.libraryItemId,
            episodeId = item.episodeId,
            itemOrder = index,
          ),
        )
      }
    }
  }

  private suspend fun handleAdd(
    mutation: PlaylistsStore.Operation.Mutation.Add,
  ) = withContext(dispatcherProvider.databaseRead) {
    val playlistCount = db.playlistItemJoinQueries
      .countForPlaylist(mutation.playlistId)
      .awaitAsOne()

    withContext(dispatcherProvider.databaseWrite) {
      db.playlistItemJoinQueries.insert(
        PlaylistItemJoin(
          playlistId = mutation.playlistId,
          libraryItemId = mutation.item.libraryItemId,
          episodeId = mutation.item.episodeId,
          itemOrder = playlistCount.toInt(),
        ),
      )
    }
  }

  private suspend fun handleRemove(
    mutation: PlaylistsStore.Operation.Mutation.Remove,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.playlistItemJoinQueries.deleteForItem(
      playlistId = mutation.playlistId,
      libraryItemId = mutation.item.libraryItemId,
    )
  }

  private suspend fun handleDelete(
    mutation: PlaylistsStore.Operation.Mutation.Delete,
  ) = withContext(dispatcherProvider.databaseWrite) {
    db.transaction {
      db.playlistItemJoinQueries.delete(mutation.id)
      db.playlistsQueries.delete(mutation.id)
    }
  }
}
