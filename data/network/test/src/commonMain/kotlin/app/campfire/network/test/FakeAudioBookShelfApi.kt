package app.campfire.network.test

import app.campfire.network.AudioBookShelfApi
import app.campfire.network.PagedResponse
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.envelopes.SyncLocalSessionsResult
import app.campfire.network.models.AudioBookmark
import app.campfire.network.models.Author
import app.campfire.network.models.Collection
import app.campfire.network.models.FilterData
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemFilter
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.LibraryStats
import app.campfire.network.models.ListeningStats
import app.campfire.network.models.MediaProgress
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.PlaybackSession
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

  override suspend fun getLibraryItems(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<List<LibraryItemExpanded>> {
    TODO("Not yet implemented")
  }

  override suspend fun getLibraryItemsMinified(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<PagedResponse<LibraryItemMinified<MinifiedBookMetadata>>> {
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

  var series: Result<List<Series>> = Result.failure(IllegalStateException("missing fake"))
  override suspend fun getSeries(libraryId: String): Result<List<Series>> {
    return series
  }

  override suspend fun getSeriesById(
    libraryId: String,
    seriesId: String,
  ): Result<Series> {
    TODO("Not yet implemented")
  }

  override suspend fun getAuthors(libraryId: String): Result<List<Author>> {
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

  override suspend fun getMediaProgress(libraryItemId: String): Result<MediaProgress> {
    TODO("Not yet implemented")
  }

  override suspend fun updateMediaProgress(
    libraryItemId: String,
    update: MediaProgressUpdatePayload,
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
}
