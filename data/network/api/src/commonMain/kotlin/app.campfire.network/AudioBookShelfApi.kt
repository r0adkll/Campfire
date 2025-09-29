package app.campfire.network

import app.campfire.network.envelopes.LoginResponse
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

interface AudioBookShelfApi {

  /**
   * Ping an audiobookshelf server to validate that it exists and is running
   */
  suspend fun ping(serverUrl: String): Boolean

  /**
   * This endpoint logs in a client to the server, returning information about the user and server.
   * @param serverUrl the url of the audiobookshelf server to call
   * @param username the [username] of the user to login as
   * @param password the password for [username] to login with
   */
  suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse>

  /**
   * Retrieve the current user information, including media progress and bookmarks
   */
  suspend fun getCurrentUser(): Result<User>

  /**
   * Fetch all the libraries accessible to the user
   * @return a result with a list of library objects
   */
  suspend fun getAllLibraries(): Result<List<Library>>

  /**
   * Fetch a single library by its [libraryId]
   * @return a result with the requested library
   */
  suspend fun getLibrary(libraryId: String): Result<Library>

  /**
   * Fetch a library's items
   * @param libraryId the id of the library to fetch the items for
   * @return as result with the list of library items
   */
  suspend fun getLibraryItems(
    libraryId: String,
    filter: LibraryItemFilter? = null,
    sortMode: String? = null,
    sortDescending: Boolean = false,
    page: Int = 0,
    limit: Int = 0,
  ): Result<List<LibraryItemExpanded>>

  /**
   * DEPRECATED
   * ----------
   * This endpoint is deprecated, and only in use while we get a fix merged upstream for returning
   * the expanded item models from the backend.
   * [https://github.com/advplyr/audiobookshelf/pull/3945](https://github.com/advplyr/audiobookshelf/pull/3945)
   *
   * Fetch a library's items
   *
   * @param libraryId the id of the library to fetch the items for
   * @return as result with the list of library items
   */
  @Deprecated(
    message = "This endpoint is deprecated since it only returns the minified model",
    replaceWith = ReplaceWith("getLibraryItems"),
  )
  suspend fun getLibraryItemsMinified(
    libraryId: String,
    filter: LibraryItemFilter? = null,
    sortMode: String? = null,
    sortDescending: Boolean = false,
    page: Int = INVALID,
    limit: Int = INVALID,
  ): Result<PagedResponse<LibraryItemMinified<MinifiedBookMetadata>>>

  /**
   * Fetch a single library item
   *
   * @param itemId the id of the item to fetch
   * @return as result with the library item with expanded details
   */
  suspend fun getLibraryItem(itemId: String): Result<LibraryItemExpanded>

  /**
   * This endpoint returns a library's stats
   */
  suspend fun getLibraryStats(libraryId: String): Result<LibraryStats>

  /**
   * Get a Library's Personalized View
   * This endpoint returns a library's personalized view for home page display.
   */
  suspend fun getPersonalizedHome(libraryId: String): Result<List<Shelf>>

  /**
   * Get a Library's list of series
   */
  suspend fun getSeries(libraryId: String): Result<List<Series>>

  /**
   * Get a Library's list of authors
   */
  suspend fun getAuthors(libraryId: String): Result<List<Author>>

  /**
   * Get a specific author
   */
  suspend fun getAuthor(authorId: String): Result<Author>

  /**
   * Get a Library's list of collections
   */
  suspend fun getCollections(libraryId: String): Result<List<Collection>>

  /**
   * Get a single collection
   */
  suspend fun getCollection(collectionId: String): Result<Collection>

  /**
   * Create a new collection
   */
  suspend fun createCollection(
    libraryId: String,
    name: String,
    description: String?,
    bookIds: List<String>,
  ): Result<Collection>

  /**
   * Update an existing collection
   */
  suspend fun updateCollection(
    collectionId: String,
    name: String? = null,
    description: String? = null,
  ): Result<Collection>

  /**
   * Add a book to an existing collection
   */
  suspend fun addBookToCollection(
    collectionId: String,
    libraryItemId: String,
  ): Result<Collection>

  /**
   * Remove a book from an existing collection
   */
  suspend fun removeBookFromCollection(
    collectionId: String,
    libraryItemId: String,
  ): Result<Collection>

  /**
   * Remove a batch of books from an existing collection
   */
  suspend fun removeBooksFromCollection(
    collectionId: String,
    libraryItemIds: List<String>,
  ): Result<Collection>

  /**
   * Delete an existing collection
   */
  suspend fun deleteCollection(collectionId: String): Result<Unit>

  /**
   * This endpoint retrieves your media progress that is associated with the given library item ID or
   * podcast episode ID.
   */
  suspend fun getMediaProgress(libraryItemId: String): Result<MediaProgress>

  /**
   * Create/Update the media progress for a specific item
   */
  suspend fun updateMediaProgress(libraryItemId: String, update: MediaProgressUpdatePayload): Result<Unit>

  /**
   * This endpoint batch creates/updates your media progress.
   */
  suspend fun batchUpdateMediaProgress(updates: List<MediaProgressUpdatePayload>): Result<Unit>

  /**
   * Remove the media progress for a given [mediaProgressId]
   */
  suspend fun deleteMediaProgress(mediaProgressId: String): Result<Unit>

  /**
   * This endpoint creates a bookmark for a book library item and returns the created bookmark.
   */
  suspend fun createBookmark(libraryItemId: String, timeInSeconds: Int, title: String): Result<AudioBookmark>

  /**
   * This endpoint removes a bookmark(
   */
  suspend fun removeBookmark(libraryItemId: String, timeInSeconds: Int): Result<Unit>

  /**
   * This endpoint creates/updates multiple local listening sessions on the server. Used for syncing offline listening
   * sessions. The client must use UUIDv4 as the id for the local listening sessions because this will be used as the
   * identifier on the server as well.
   */
  suspend fun syncLocalSessions(sessions: List<PlaybackSession>): Result<SyncLocalSessionsResult>

  /**
   * This endpoint creates/updates a local listening session on the server. Used for syncing offline listening
   */
  suspend fun syncLocalSession(session: PlaybackSession): Result<Unit>

  /**
   * This endpoint searches a library for the query and returns the results.
   */
  suspend fun searchLibrary(libraryId: String, query: String): Result<SearchResult>

  /**
   * This endpoint retrieves a user's listening statistics.
   */
  suspend fun getListeningStats(): Result<ListeningStats>

  /**
   * Get all the data that the user can filter the list of library items with
   */
  suspend fun getFilterData(libraryId: String): Result<FilterData>
}

const val INVALID = -1
