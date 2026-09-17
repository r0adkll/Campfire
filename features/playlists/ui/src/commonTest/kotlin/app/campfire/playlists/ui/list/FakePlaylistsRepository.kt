// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import app.campfire.core.model.CollectionId
import app.campfire.core.model.Playlist
import app.campfire.core.model.PlaylistId
import app.campfire.playlists.api.PlaylistsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakePlaylistsRepository(
  private val onRefreshPlaylists: suspend () -> Unit = {},
) : PlaylistsRepository {

  var refreshCount = 0
    private set

  override fun observeAllPlaylists(): Flow<List<Playlist>> = emptyFlow()

  override suspend fun refreshPlaylists() {
    refreshCount++
    onRefreshPlaylists()
  }

  override fun observePlaylist(playlistId: PlaylistId, isCreatedId: Boolean): Flow<Playlist> = TODO()

  override fun observePlaylistItems(playlistId: PlaylistId): Flow<List<Playlist.Item.Expanded>> = TODO()

  override suspend fun createPlaylist(
    name: String,
    description: String?,
    items: List<Playlist.Item.Minified>,
  ): Result<PlaylistId> = TODO()

  override suspend fun updatePlaylist(
    playlistId: PlaylistId,
    name: String,
    description: String?,
    items: List<Playlist.Item.Minified>,
  ): Result<Unit> = TODO()

  override suspend fun deletePlaylist(playlistId: PlaylistId): Result<Unit> = TODO()

  override suspend fun addToPlaylist(playlistId: PlaylistId, item: Playlist.Item.Minified): Result<Unit> = TODO()

  override suspend fun removeFromPlaylist(playlistId: PlaylistId, item: Playlist.Item.Minified): Result<Unit> = TODO()

  override suspend fun createPlaylistFromCollection(collectionId: CollectionId): Result<PlaylistId> = TODO()
}
