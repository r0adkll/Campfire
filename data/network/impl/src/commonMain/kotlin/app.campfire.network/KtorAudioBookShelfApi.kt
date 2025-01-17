package app.campfire.network

import app.campfire.account.api.AccountManager
import app.campfire.account.api.UserSessionManager
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.session.serverUrl
import app.campfire.network.envelopes.AllLibrariesResponse
import app.campfire.network.envelopes.AuthorResponse
import app.campfire.network.envelopes.CollectionsResponse
import app.campfire.network.envelopes.CreateBookmarkRequest
import app.campfire.network.envelopes.LibraryItemsResponse
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.envelopes.MediaProgressUpdatePayload
import app.campfire.network.envelopes.PingResponse
import app.campfire.network.envelopes.SeriesResponse
import app.campfire.network.envelopes.SyncLocalSessionsResult
import app.campfire.network.envelopes.SyncSessionRequest
import app.campfire.network.models.AudioBookmark
import app.campfire.network.models.Author
import app.campfire.network.models.Collection
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MediaProgress
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.NetworkModel
import app.campfire.network.models.PlaybackSession
import app.campfire.network.models.Series
import app.campfire.network.models.Shelf
import app.campfire.network.models.User
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesBinding(AppScope::class)
class KtorAudioBookShelfApi(
  private val httpClient: HttpClient,
  private val accountManager: AccountManager,
  private val userSessionManager: UserSessionManager,
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

  override suspend fun ping(
    serverUrl: String,
  ): Boolean = trySendRequest<PingResponse> { client.get(Url("$serverUrl/ping")) }
    .map { it.success }
    .getOrElse { false }

  override suspend fun login(
    serverUrl: String,
    username: String,
    password: String,
  ): Result<LoginResponse> = trySendRequest {
    client.post {
      url("${cleanServerUrl(serverUrl)}/login")
      contentType(ContentType.Application.Json)
      setBody(LoginRequest(username, password))
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
    filter: String?,
  ): Result<List<LibraryItemMinified<MinifiedBookMetadata>>> {
    return trySendRequest<LibraryItemsResponse> {
      hydratedClientRequest({
        appendPathSegments("api", "libraries", libraryId, "items")
        filter?.let { f -> parameters.append("filter", f) }
      })
    }.map { it.results }
  }

  override suspend fun getLibraryItem(itemId: String): Result<LibraryItemExpanded> {
    return trySendRequest<LibraryItemExpanded> {
      hydratedClientRequest("/api/items/$itemId?expanded=1&include=progress,authors,downloads")
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
          body.origin = RequestOrigin.Url(originServerUrl)
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
    val currentServerUrl = userSessionManager.current.serverUrl
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val token = accountManager.getToken(currentServerUrl)
      ?: throw IllegalStateException("No authentication found for the url $currentServerUrl")
    return client.request {
      url("${cleanServerUrl(currentServerUrl)}${if (!endpoint.startsWith("/")) "/" else ""}$endpoint")
      header(HttpHeaders.Authorization, "Bearer $token")
      header(HEADER_SERVER_URL, currentServerUrl)
      contentType(ContentType.Application.Json)
      builder()
    }
  }

  private suspend fun hydratedClientRequest(
    urlBuilder: URLBuilder.() -> Unit,
    builder: HttpRequestBuilder.() -> Unit = { },
  ): HttpResponse {
    val currentServerUrl = userSessionManager.current.serverUrl
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val token = accountManager.getToken(currentServerUrl)
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

  private fun cleanServerUrl(url: String): String {
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

  companion object {
    private const val HEADER_SERVER_URL = "X-Server-Url"
  }
}
