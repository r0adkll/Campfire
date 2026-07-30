// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists

import app.campfire.CampfireDatabase
import app.campfire.account.api.UrlHydrator
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.SingleIn
import app.campfire.core.di.UserScope
import app.campfire.core.model.CollectionId
import app.campfire.core.model.Media
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.data.mapping.asDomainModel
import app.campfire.data.mapping.dao.LibraryItemDao
import app.campfire.data.mapping.store.debugLogging
import app.campfire.network.models.PlaylistExpanded
import app.campfire.playlists.api.PlaylistsRepository
import app.campfire.playlists.store.PlaylistsStore
import app.campfire.user.api.UserRepository
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import me.tatarka.inject.annotations.Inject
import org.mobilenativefoundation.store.store5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreReadResponseOrigin
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.StoreWriteResponse

@OptIn(ExperimentalStoreApi::class, ExperimentalCoroutinesApi::class)
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class StorePlaylistsRepository(
  private val userRepository: UserRepository,
  private val db: CampfireDatabase,
  private val urlHydrator: UrlHydrator,
  private val libraryItemDao: LibraryItemDao,
  private val storeFactory: PlaylistsStore.Factory,
  private val dispatcherProvider: DispatcherProvider,
) : PlaylistsRepository {

  private val playlistsStore by lazy { storeFactory.create() }

  override fun observeAllPlaylists(): Flow<List<Playlist>> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->

        val operation = PlaylistsStore.Operation.All(user.id, user.selectedLibraryId)
        val request = StoreReadRequest.cached(operation, refresh = true)

        playlistsStore.stream<StoreReadResponse<PlaylistsStore.Output>>(request)
          .debugLogging("PlaylistsStore::observeAllPlaylists")
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            response.dataOrNull()?.let { output ->
              // If the response is empty, and from the SoT then lets just return null and wait
              // for the network request to return.
              if (output.isEmpty() && response.origin == StoreReadResponseOrigin.SourceOfTruth) {
                PlaylistsStore.dbark { "Output is empty and response is from SoT, force network fetch." }
                return@mapNotNull null
              }

              (output as PlaylistsStore.Output.Collection).playlists
            }
          }
      }
  }

  override fun observePlaylist(
    playlistId: PlaylistId,
    isCreatedId: Boolean,
  ): Flow<Playlist> {
    return userRepository.observeCurrentUser()
      .flatMapLatest { user ->
        val operation = PlaylistsStore.Operation.Single(
          userId = user.id,
          libraryId = user.selectedLibraryId,
          playlistId = playlistId,
          isCreatedId = isCreatedId,
        )
        val request = StoreReadRequest.cached(operation, refresh = false)

        playlistsStore.stream<StoreReadResponse<PlaylistsStore.Output>>(request)
          .debugLogging("PlaylistsStore::observePlaylist")
          .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
          .mapNotNull { response ->
            val output = response.dataOrNull() as? PlaylistsStore.Output.Single
            output?.playlist
          }
      }
  }

  override fun observePlaylistItems(playlistId: PlaylistId): Flow<List<Playlist.Item.Expanded>> {
    return db.playlistItemJoinQueries
      .selectForPlaylist(playlistId)
      .asFlow()
      .mapToList(dispatcherProvider.databaseRead)
      .mapLatest { junctionEntries ->
        junctionEntries.mapIndexedNotNull { index, junction ->
          val libraryItem = libraryItemDao.hydrateById(junction.libraryItemId)
            ?: return@mapIndexedNotNull null
          val episodeId = junction.episodeId.takeIf { it.isNotEmpty() }
          val episode = episodeId?.let { id ->
            // Prefer the in-memory podcast cache; fall back to a direct DB lookup so we
            // resolve the episode even when the parent podcast hasn't been fully fetched
            // (the playlist endpoint returns a *minified* podcast with no episodes array).
            (libraryItem.media as? Media.Podcast)
              ?.episodes
              ?.firstOrNull { it.id == id }
              ?: db.podcastEpisodeQueries
                .selectForId(id)
                .awaitAsOneOrNull()
                ?.let { row ->
                  val track = db.podcastEpisodeAudioTrackQueries
                    .selectForEpisodeId(id)
                    .awaitAsOneOrNull()
                    ?.asDomainModel(urlHydrator)
                  row.asDomainModel(audioTrack = track)
                }
          }
          Playlist.Item.Expanded(
            index = index,
            libraryItemId = junction.libraryItemId,
            episodeId = episodeId,
            libraryItem = libraryItem,
            episode = episode,
          )
        }
      }
  }

  override suspend fun createPlaylist(
    name: String,
    description: String?,
    items: List<Playlist.Item.Minified>,
  ): Result<PlaylistId> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.Create(
      userId = currentUser.id,
      name = name,
      description = description,
      libraryId = currentUser.selectedLibraryId,
      items = items,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        PlaylistsStore.ibark { "Playlist Create -> $response" }
        Result.success(operation.creationId)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun updatePlaylist(
    playlistId: PlaylistId,
    name: String,
    description: String?,
    items: List<Playlist.Item.Minified>,
  ): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.Update(
      userId = currentUser.id,
      id = playlistId,
      name = name,
      description = description,
      items = items,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        PlaylistsStore.ibark { "Playlist Updated -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun deletePlaylist(playlistId: PlaylistId): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.Delete(
      userId = currentUser.id,
      id = playlistId,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        PlaylistsStore.ibark { "Playlist Deleted -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun addToPlaylist(playlistId: PlaylistId, item: Playlist.Item.Minified): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.Add(
      userId = currentUser.id,
      playlistId = playlistId,
      item = item,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        PlaylistsStore.ibark { "Item Added -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun removeFromPlaylist(playlistId: PlaylistId, item: Playlist.Item.Minified): Result<Unit> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.Remove(
      userId = currentUser.id,
      playlistId = playlistId,
      item = item,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success -> {
        PlaylistsStore.ibark { "Item Removed -> $response" }
        Result.success(Unit)
      }

      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }

  override suspend fun createPlaylistFromCollection(collectionId: CollectionId): Result<PlaylistId> {
    val currentUser = userRepository.getCurrentUser()

    val operation = PlaylistsStore.Operation.Mutation.FromCollection(
      userId = currentUser.id,
      collectionId = collectionId,
      libraryId = currentUser.selectedLibraryId,
    )

    val response = playlistsStore.write(
      request = StoreWriteRequest.of(operation, PlaylistsStore.Output.Collection(emptyList())),
    )

    return when (response) {
      is StoreWriteResponse.Success.Typed<*> -> {
        PlaylistsStore.ibark { "Playlist from Collection -> $response" }

        @Suppress("UNCHECKED_CAST")
        val newPlaylist = response.value as PlaylistExpanded
        storeFactory.sourceOfTruthFactory
          .writeSingle(
            userId = currentUser.id,
            libraryId = currentUser.selectedLibraryId,
            playlist = newPlaylist.asDomainModel(urlHydrator),
          )

        Result.success(newPlaylist.id)
      }

      is StoreWriteResponse.Success.Untyped -> Result.failure(Exception("Unknown response type"))
      is StoreWriteResponse.Error.Message -> Result.failure(Exception(response.message))
      is StoreWriteResponse.Error.Exception -> Result.failure(response.error)
    }
  }
}
