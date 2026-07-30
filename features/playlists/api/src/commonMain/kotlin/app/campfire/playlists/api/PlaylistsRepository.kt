// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.api

import app.campfire.core.model.CollectionId
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import kotlinx.coroutines.flow.Flow

interface PlaylistsRepository {

  /**
   * Observe the list of [Playlist] for the current library
   * @return a flow of all [Playlist]s
   */
  fun observeAllPlaylists(): Flow<List<Playlist>>

  /**
   * Observe a single playlist
   * @param playlistId the id of the playlist to observe
   * @param isCreatedId the [playlistId] is a locally created id from a new playlist
   * @return a flow of the [Playlist]
   */
  fun observePlaylist(
    playlistId: PlaylistId,
    isCreatedId: Boolean = false,
  ): Flow<Playlist>

  /**
   * Observe the items of a playlist as [Playlist.Item.Expanded]. Each entry carries the
   * resolved [app.campfire.core.model.LibraryItem] and, for podcast entries, the specific
   * [app.campfire.core.model.PodcastEpisode] referenced by the playlist.
   */
  fun observePlaylistItems(playlistId: PlaylistId): Flow<List<Playlist.Item.Expanded>>

  /**
   * Create a new playlist
   * @param name the name of the playlist
   * @param description the description of the playlist
   * @param items the items to add to the playlist
   * @return the id of the newly created [Playlist]
   */
  suspend fun createPlaylist(
    name: String,
    description: String? = null,
    items: List<Playlist.Item.Minified> = emptyList(),
  ): Result<PlaylistId>

  /**
   * Update a playlist
   * @param playlistId the id of the playlist to update
   * @param name the new name of the playlist
   * @param description the new description of the playlist
   * @param items the updated full list of items
   * @return the result of the update
   */
  suspend fun updatePlaylist(
    playlistId: PlaylistId,
    name: String,
    description: String? = null,
    items: List<Playlist.Item.Minified>,
  ): Result<Unit>

  /**
   * Delete a playlist
   * @param playlistId the id of the playlist to delete
   * @return the result of the delete
   */
  suspend fun deletePlaylist(
    playlistId: PlaylistId,
  ): Result<Unit>

  /**
   * Add an item to a playlist
   * @param playlistId the id of the playlist
   * @param item the item to add
   * @return the result of the add
   */
  suspend fun addToPlaylist(
    playlistId: PlaylistId,
    item: Playlist.Item.Minified,
  ): Result<Unit>

  /**
   * Remove an item from a playlist
   * @param playlistId the id of the playlist
   * @param item the item to remove
   * @return the result of the remove
   */
  suspend fun removeFromPlaylist(
    playlistId: PlaylistId,
    item: Playlist.Item.Minified,
  ): Result<Unit>

  /**
   * Create a new playlist from an existing collection
   * @param collectionId the id of the collection to create the playlist from
   * @return the id of the newly created [Playlist]
   */
  suspend fun createPlaylistFromCollection(
    collectionId: CollectionId,
  ): Result<PlaylistId>
}
