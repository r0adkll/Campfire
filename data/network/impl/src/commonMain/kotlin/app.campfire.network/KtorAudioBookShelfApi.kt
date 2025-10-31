package app.campfire.network

import app.campfire.account.api.AccountManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.session.UserSession
import app.campfire.core.session.serverUrl
import app.campfire.core.session.userId
import app.campfire.network.envelopes.AddBookToCollectionRequest
import app.campfire.network.envelopes.AllLibrariesResponse
import app.campfire.network.envelopes.AuthorResponse
import app.campfire.network.envelopes.BatchBooksRequest
import app.campfire.network.envelopes.CollectionsResponse
import app.campfire.network.envelopes.CreateBookmarkRequest
import app.campfire.network.envelopes.LibraryItemsResponse
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.envelopes.MinifiedLibraryItemsResponse
import app.campfire.network.envelopes.NewCollectionRequest
import app.campfire.network.envelopes.SeriesResponse
import app.campfire.network.envelopes.SyncLocalSessionsResult
import app.campfire.network.envelopes.SyncSessionRequest
import app.campfire.network.envelopes.UpdateCollectionRequest
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
import app.campfire.network.models.NetworkModel
import app.campfire.network.models.PlaybackSession
import app.campfire.network.models.SearchResult
import app.campfire.network.models.Series
import app.campfire.network.models.Shelf
import app.campfire.network.models.User
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesBinding(UserScope::class)
class KtorAudioBookShelfApi(
  private val userSession: UserSession,
  private val httpClient: HttpClient,
  private val accountManager: AccountManager,
  private val dispatcherProvider: DispatcherProvider,
) : AudioBookShelfApi {

  private val client by lazy {
    httpClient.config {
      defaultRequest {
      }

      install(ContentNegotiation) {
        json()
      }
    }
  }

  override suspend fun getCurrentUser(): Result<User> = trySendRequest {
    hydratedClientRequest("/api/me")
  }

  override suspend fun getAllLibraries(): Result<List<Library>> = trySendRequest<AllLibrariesResponse> {
    hydratedClientRequest("/api/libraries")
  }.map { it.libraries }

  override suspend fun getLibrary(libraryId: String): Result<Library> = trySendRequest<Library> {
    hydratedClientRequest("/api/libraries/$libraryId")
  }

  override suspend fun getLibraryItems(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<List<LibraryItemExpanded>> {
    return trySendRequest<LibraryItemsResponse> {
      hydratedClientRequest({
        appendPathSegments("api", "libraries", libraryId, "items")
        parameters.append("minified", "0")
        filter?.let { f ->
          val filterValue = "${f.group}.${f.value.encodeBase64().encodeURLQueryComponent()}"
          parameters.append("filter", filterValue)
        }
        sortMode?.let { parameters.append("sort", it) }
        if (sortDescending) parameters.append("sort_desc", "1")
        parameters.append("page", page.toString())
        parameters.append("limit", limit.toString())
      })
    }.map { it.results }
  }

  @Deprecated(
    "This endpoint is deprecated since it only returns the minified model",
    replaceWith = ReplaceWith("getLibraryItems"),
  )
  override suspend fun getLibraryItemsMinified(
    libraryId: String,
    filter: LibraryItemFilter?,
    sortMode: String?,
    sortDescending: Boolean,
    page: Int,
    limit: Int,
  ): Result<PagedResponse<LibraryItemMinified<MinifiedBookMetadata>>> {
    return trySendRequest<MinifiedLibraryItemsResponse> {
      hydratedClientRequest({
        appendPathSegments("api", "libraries", libraryId, "items")
        parameters.append("minified", "1")
        filter?.let { f ->
          val filterValue = "${f.group}.${f.value.encodeBase64().encodeURLQueryComponent()}"
          parameters.append("filter", filterValue)
        }
        sortMode?.let { parameters.append("sort", it) }
        if (sortDescending) parameters.append("sort_desc", "1")
        if (page != INVALID) parameters.append("page", page.toString())
        if (limit != INVALID) parameters.append("limit", limit.toString())
      })
    }.map {
      PagedResponse(
        data = it.results,
        page = it.page,
        limit = it.limit,
        total = it.total,
        offset = it.offset,
      )
    }
  }

  override suspend fun getLibraryItem(itemId: String): Result<LibraryItemExpanded> {
    return trySendRequest<LibraryItemExpanded> {
      hydratedClientRequest("/api/items/$itemId?expanded=1&include=progress,authors,downloads")
    }
  }

  override suspend fun getLibraryStats(libraryId: String): Result<LibraryStats> {
    return trySendRequest {
      hydratedClientRequest("/api/libraries/$libraryId/stats")
    }
  }

  override suspend fun getPersonalizedHome(libraryId: String): Result<List<Shelf>> {
    return trySendRequest<List<Shelf>> {
      hydratedClientRequest("/api/libraries/$libraryId/personalized")
    }
  }

  override suspend fun getSeries(libraryId: String): Result<List<Series>> {
    return trySendRequest<SeriesResponse> {
      hydratedClientRequest("/api/libraries/$libraryId/series?limit=1000")
    }.map { it.results }
  }

  override suspend fun getAuthors(libraryId: String): Result<List<Author>> {
    return trySendRequest<AuthorResponse> {
      hydratedClientRequest("/api/libraries/$libraryId/authors")
    }.map { it.authors }
  }

  override suspend fun getAuthor(authorId: String): Result<Author> {
    return trySendRequest<Author> {
      hydratedClientRequest("/api/authors/$authorId?include=items")
    }
  }

  override suspend fun getCollections(libraryId: String): Result<List<Collection>> {
    return trySendRequest<CollectionsResponse> {
      hydratedClientRequest("/api/libraries/$libraryId/collections")
    }.map { it.results }
  }

  override suspend fun getCollection(collectionId: String): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections/$collectionId")
    }
  }

  override suspend fun createCollection(
    libraryId: String,
    name: String,
    description: String?,
    bookIds: List<String>,
  ): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections") {
        method = HttpMethod.Post
        setBody(
          NewCollectionRequest(
            libraryId = libraryId,
            name = name,
            description = description,
            books = bookIds,
          ),
        )
      }
    }
  }

  override suspend fun updateCollection(
    collectionId: String,
    name: String?,
    description: String?,
  ): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections/$collectionId") {
        method = HttpMethod.Patch
        setBody(
          UpdateCollectionRequest(
            name = name,
            description = description,
          ),
        )
      }
    }
  }

  override suspend fun addBookToCollection(collectionId: String, libraryItemId: String): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections/$collectionId/book") {
        method = HttpMethod.Post
        setBody(AddBookToCollectionRequest(libraryItemId))
      }
    }
  }

  override suspend fun removeBookFromCollection(collectionId: String, libraryItemId: String): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections/$collectionId/book/$libraryItemId") {
        method = HttpMethod.Delete
      }
    }
  }

  override suspend fun removeBooksFromCollection(
    collectionId: String,
    libraryItemIds: List<String>,
  ): Result<Collection> {
    return trySendRequest {
      hydratedClientRequest("/api/collections/$collectionId/batch/remove") {
        method = HttpMethod.Post
        setBody(BatchBooksRequest(libraryItemIds))
      }
    }
  }

  override suspend fun deleteCollection(collectionId: String): Result<Unit> {
    return trySendRequest({}) {
      hydratedClientRequest("/api/collections/$collectionId") {
        method = HttpMethod.Delete
      }
    }
  }

  override suspend fun getMediaProgress(libraryItemId: String): Result<MediaProgress> {
    return trySendRequest {
      hydratedClientRequest("/api/me/progress/$libraryItemId")
    }
  }

  override suspend fun updateMediaProgress(libraryItemId: String, update: MediaProgressUpdatePayload): Result<Unit> {
    return trySendRequest({}) {
      hydratedClientRequest("/api/me/progress/$libraryItemId") {
        method = HttpMethod.Patch
        setBody(update)
      }
    }
  }

  override suspend fun batchUpdateMediaProgress(updates: List<MediaProgressUpdatePayload>): Result<Unit> {
    return trySendRequest({}) {
      hydratedClientRequest("/api/me/progress/batch/update") {
        method = HttpMethod.Patch
        setBody(updates)
      }
    }
  }

  override suspend fun deleteMediaProgress(mediaProgressId: String): Result<Unit> {
    return trySendRequest({}) {
      hydratedClientRequest("/api/me/progress/$mediaProgressId") {
        method = HttpMethod.Delete
      }
    }
  }

  override suspend fun createBookmark(libraryItemId: String, timeInSeconds: Int, title: String): Result<AudioBookmark> {
    return trySendRequest {
      hydratedClientRequest("/api/me/item/$libraryItemId/bookmark") {
        method = HttpMethod.Post
        setBody(
          CreateBookmarkRequest(
            time = timeInSeconds,
            title = title,
          ),
        )
      }
    }
  }

  override suspend fun removeBookmark(libraryItemId: String, timeInSeconds: Int): Result<Unit> {
    return trySendRequest({}) {
      hydratedClientRequest("/api/me/item/$libraryItemId/bookmark/$timeInSeconds") {
        method = HttpMethod.Delete
      }
    }
  }

  override suspend fun syncLocalSessions(
    sessions: List<PlaybackSession>,
  ): Result<SyncLocalSessionsResult> {
    return trySendRequest<SyncLocalSessionsResult> {
      hydratedClientRequest("/api/session/local-all") {
        method = HttpMethod.Post
        setBody(SyncSessionRequest(sessions))
      }
    }
  }

  override suspend fun syncLocalSession(session: PlaybackSession): Result<Unit> {
    return trySendRequest(
      responseMapper = {},
    ) {
      hydratedClientRequest("/api/session/local") {
        method = HttpMethod.Post
        setBody(session)
      }
    }
  }

  override suspend fun searchLibrary(libraryId: String, query: String): Result<SearchResult> {
    return trySendRequest {
      hydratedClientRequest("api/libraries/$libraryId/search?q=${query.encodeURLQueryComponent()}")
    }
  }

  override suspend fun getListeningStats(): Result<ListeningStats> {
    val currentUserId = userSession.userId ?: return Result.failure(NotLoggedInException())
    return trySendRequest {
      hydratedClientRequest("api/users/$currentUserId/listening-stats")
    }
  }

  override suspend fun getFilterData(libraryId: String): Result<FilterData> {
    return trySendRequest {
      hydratedClientRequest("api/libraries/$libraryId/filterdata")
    }
  }

  private suspend inline fun <reified T> trySendRequest(
    noinline responseMapper: suspend (HttpResponse) -> T = { it.body<T>() },
    crossinline request: suspend () -> HttpResponse,
  ): Result<T> = withContext(dispatcherProvider.io) {
    try {
      val response = request()
      if (response.status.isSuccess()) {
        val originServerUrl = response.call.request.headers[HEADER_SERVER_URL]
        val body = responseMapper(response)
        if (body is NetworkModel && originServerUrl != null) {
          body.applyOrigin(RequestOrigin.Url(originServerUrl))
        }

        // If our response is an iterable, check each item to attach
        // metadata.
        if (body is Iterable<*> && originServerUrl != null) {
          val originUrl = RequestOrigin.Url(originServerUrl)
          body.forEach {
            if (it is NetworkModel) {
              it.applyOrigin(originUrl)
            }
          }
        }

        Result.success(body)
      } else {
        Result.failure(ApiException(response.status.value, response.bodyAsText()))
      }
    } catch (e: IOException) {
      e.printStackTrace()
      Result.failure(e)
    }
  }

  private suspend fun hydratedClientRequest(
    endpoint: String,
    builder: HttpRequestBuilder.() -> Unit = { },
  ): HttpResponse {
    val currentServerUrl = userSession.serverUrl
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val currentUserId = userSession.userId
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val token = accountManager.getToken(currentUserId)

    return client.request {
      url("${cleanServerUrl(currentServerUrl)}${if (!endpoint.startsWith("/")) "/" else ""}$endpoint")
      token?.let {
        header(HttpHeaders.Authorization, "Bearer $it")
      }
      header(HEADER_SERVER_URL, currentServerUrl)
      contentType(ContentType.Application.Json)
      builder()
    }
  }

  private suspend fun hydratedClientRequest(
    urlBuilder: URLBuilder.() -> Unit,
    builder: HttpRequestBuilder.() -> Unit = { },
  ): HttpResponse {
    val currentServerUrl = userSession.serverUrl
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val currentUserId = userSession.userId
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val token = accountManager.getToken(currentUserId)
      ?: throw IllegalStateException("No authentication found for the url $currentServerUrl")
    return client.request {
      url {
        takeFrom(cleanServerUrl(currentServerUrl))
        urlBuilder()
      }
      header(HttpHeaders.Authorization, "Bearer $token")
      header(HEADER_SERVER_URL, currentServerUrl)
      contentType(ContentType.Application.Json)
      builder()
    }
  }

  companion object {
    internal const val HEADER_SERVER_URL = "X-Server-Url"
  }
}

class NotLoggedInException : Exception()

internal fun cleanServerUrl(url: String): String {
  fun String.withoutFinalSlash(): String = if (last() == '/') {
    substringBeforeLast('/')
  } else {
    this
  }

  return if (url.startsWith("http://") || url.startsWith("https://")) {
    url.withoutFinalSlash()
  } else {
    "https://${url.withoutFinalSlash()}"
  }
}
