// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.test

import app.campfire.network.AudioBookShelfApi
import app.campfire.network.PagedResponse
import app.campfire.network.envelopes.EpisodeDownloadsResponse
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.envelopes.SyncLocalSessionsResult
import app.campfire.network.models.AudioBookmark
import app.campfire.network.models.Author
import app.campfire.network.models.Collection
import app.campfire.network.models.DeviceInfo
import app.campfire.network.models.FilterData
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.LibraryStats
import app.campfire.network.models.ListeningStats
import app.campfire.network.models.MediaProgress
import app.campfire.network.models.PagedRecentEpisodesResponse
import app.campfire.network.models.PlaySession
import app.campfire.network.models.PlaybackSession
import app.campfire.network.models.PlaylistExpanded
import app.campfire.network.models.PlaylistItem
import app.campfire.network.models.PodcastFeed
import app.campfire.network.models.PodcastMetadata
import app.campfire.network.models.PodcastSearchResultDto
import app.campfire.network.models.RssPodcastEpisode
import app.campfire.network.models.SearchResult
import app.campfire.network.models.Series
import app.campfire.network.models.Shelf
import app.campfire.network.models.User

class FakeAudioBookShelfApi : AudioBookShelfApi {

  override suspend fun getCurrentUser(): Result<User> {
    TODO("Not yet implemented")
  }

  override suspend fun getAllLibraries(): Result<List<Library>> {
    TODO("Not yet implemented")
  }

  override suspend fun getLibrary(libraryId: String): Result<Library> {
    TODO("Not yet implemented")
  }

  override suspend fun getLibraryItemsMinified(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<PagedResponse<LibraryItemMinified>> {
    TODO("Not yet implemented")
  }

  override suspend fun getLibraryItem(itemId: String): Result<LibraryItemExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun getLibraryStats(libraryId: String): Result<LibraryStats> {
    TODO("Not yet implemented")
  }

  override suspend fun getPersonalizedHome(libraryId: String): Result<List<Shelf>> {
    TODO("Not yet implemented")
  }

  override suspend fun getRecentEpisodes(
    libraryId: String,
    page: Int,
    limit: Int,
  ): Result<PagedRecentEpisodesResponse> {
    TODO("Not yet implemented")
  }

  override suspend fun getPodcastFeed(
    rssFeedUrl: String,
  ): Result<PodcastFeed> {
    TODO("Not yet implemented")
  }

  override suspend fun searchPodcasts(
    term: String,
    country: String?,
  ): Result<List<PodcastSearchResultDto>> {
    TODO("Not yet implemented")
  }

  override suspend fun createPodcast(
    libraryId: String,
    folderId: String,
    path: String,
    metadata: PodcastMetadata,
    tags: List<String>,
    autoDownloadEpisodes: Boolean,
    autoDownloadSchedule: String?,
  ): Result<LibraryItemExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun downloadPodcastEpisodes(
    libraryItemId: String,
    episodes: List<RssPodcastEpisode>,
  ): Result<Unit> {
    TODO("Not yet implemented")
  }

  var series: Result<PagedResponse<Series>> = Result.failure(IllegalStateException("missing fake"))
  override suspend fun getSeries(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<PagedResponse<Series>> {
    return series
  }

  override suspend fun getSeriesById(
    libraryId: String,
    seriesId: String,
  ): Result<Series> {
    TODO("Not yet implemented")
  }

  override suspend fun getAuthors(
    libraryId: String,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<PagedResponse<Author>> {
    TODO("Not yet implemented")
  }

  override suspend fun getAuthor(authorId: String): Result<Author> {
    TODO("Not yet implemented")
  }

  override suspend fun getCollections(libraryId: String): Result<List<Collection>> {
    TODO("Not yet implemented")
  }

  override suspend fun getCollection(collectionId: String): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun createCollection(
    libraryId: String,
    name: String,
    description: String?,
    bookIds: List<String>,
  ): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun updateCollection(
    collectionId: String,
    name: String?,
    description: String?,
  ): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun addBookToCollection(
    collectionId: String,
    libraryItemId: String,
  ): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun removeBookFromCollection(
    collectionId: String,
    libraryItemId: String,
  ): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun removeBooksFromCollection(
    collectionId: String,
    libraryItemIds: List<String>,
  ): Result<Collection> {
    TODO("Not yet implemented")
  }

  override suspend fun deleteCollection(collectionId: String): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun deleteLibraryItem(itemId: String, hard: Boolean): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun createPlaylist(
    libraryId: String,
    name: String,
    description: String?,
    items: List<PlaylistItem.Minified>,
  ): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun getPlaylists(libraryId: String): Result<List<PlaylistExpanded>> {
    TODO("Not yet implemented")
  }

  override suspend fun getPlaylist(playlistId: String): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun updatePlaylist(
    playlistId: String,
    name: String,
    description: String?,
    items: List<PlaylistItem.Minified>,
  ): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun addToPlaylist(
    playlistId: String,
    item: PlaylistItem.Minified,
  ): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun removeFromPlaylist(
    playlistId: String,
    item: PlaylistItem.Minified,
  ): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun createPlaylistFromCollection(collectionId: String): Result<PlaylistExpanded> {
    TODO("Not yet implemented")
  }

  override suspend fun getMediaProgress(
    libraryItemId: String,
    episodeId: String?,
  ): Result<MediaProgress> {
    TODO("Not yet implemented")
  }

  override suspend fun updateMediaProgress(
    libraryItemId: String,
    update: MediaProgressUpdatePayload,
    episodeId: String?,
  ): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun batchUpdateMediaProgress(updates: List<MediaProgressUpdatePayload>): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun deleteMediaProgress(mediaProgressId: String): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun createBookmark(
    libraryItemId: String,
    timeInSeconds: Int,
    title: String,
  ): Result<AudioBookmark> {
    TODO("Not yet implemented")
  }

  override suspend fun removeBookmark(
    libraryItemId: String,
    timeInSeconds: Int,
  ): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun syncLocalSessions(sessions: List<PlaybackSession>): Result<SyncLocalSessionsResult> {
    TODO("Not yet implemented")
  }

  override suspend fun syncLocalSession(session: PlaybackSession): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun startPlaybackSession(
    libraryItemId: String,
    episodeId: String?,
    deviceInfo: DeviceInfo,
    mediaPlayer: String,
    supportedMimeTypes: List<String>,
    forceDirectPlay: Boolean,
    forceTranscode: Boolean,
  ): Result<PlaySession> {
    TODO("Not yet implemented")
  }

  override suspend fun syncPlaybackSession(
    sessionId: String,
    currentTime: Double,
    timeListened: Double,
    duration: Double,
  ): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun closePlaybackSession(
    sessionId: String,
    currentTime: Double?,
    timeListened: Double?,
    duration: Double?,
  ): Result<Unit> {
    TODO("Not yet implemented")
  }

  override suspend fun searchLibrary(
    libraryId: String,
    query: String,
  ): Result<SearchResult> {
    TODO("Not yet implemented")
  }

  override suspend fun getListeningStats(): Result<ListeningStats> {
    TODO("Not yet implemented")
  }

  override suspend fun getFilterData(libraryId: String): Result<FilterData> {
    TODO("Not yet implemented")
  }

  override suspend fun getEpisodeDownloads(libraryId: String): Result<EpisodeDownloadsResponse> {
    TODO("Not yet implemented")
  }

  override suspend fun clearPodcastDownloadQueue(libraryItemId: String): Result<Unit> {
    TODO("Not yet implemented")
  }
}
