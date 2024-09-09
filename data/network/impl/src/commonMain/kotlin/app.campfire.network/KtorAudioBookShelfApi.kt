package app.campfire.network

import app.campfire.account.api.AccountManager
import app.campfire.common.settings.CampfireSettings
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.network.envelopes.AllLibrariesResponse
import app.campfire.network.envelopes.AuthorResponse
import app.campfire.network.envelopes.CollectionsResponse
import app.campfire.network.envelopes.LibraryItemsResponse
import app.campfire.network.envelopes.LoginRequest
import app.campfire.network.envelopes.LoginResponse
import app.campfire.network.envelopes.PingResponse
import app.campfire.network.envelopes.SeriesResponse
import app.campfire.network.models.Author
import app.campfire.network.models.Collection
import app.campfire.network.models.Library
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.LibraryItemMinified
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.Series
import app.campfire.network.models.Shelf
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
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import me.tatarka.inject.annotations.Inject

@Inject
@ContributesBinding(AppScope::class)
class KtorAudioBookShelfApi(
  private val httpClient: HttpClient,
  private val settings: CampfireSettings,
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

  override suspend fun getAllLibraries(): Result<List<Library>> = trySendRequest<AllLibrariesResponse> {
    hydratedClientRequest("/api/libraries")
  }.map { it.libraries }

  override suspend fun getLibrary(libraryId: String): Result<Library> = trySendRequest<Library> {
    hydratedClientRequest("/api/libraries/$libraryId")
  }

  override suspend fun getLibraryItems(libraryId: String): Result<List<LibraryItemMinified<MinifiedBookMetadata>>> {
    return trySendRequest<LibraryItemsResponse> {
      hydratedClientRequest("/api/libraries/$libraryId/items")
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

  private suspend inline fun <reified T> trySendRequest(
    noinline responseMapper: suspend (HttpResponse) -> T = { it.body<T>() },
    crossinline request: suspend () -> HttpResponse,
  ): Result<T> = withContext(dispatcherProvider.io) {
    try {
      val response = request()
      if (response.status.isSuccess()) {
        Result.success(responseMapper(response))
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
    val currentServerUrl = settings.currentServerUrl
      ?: throw IllegalStateException("You must be logged in to perform this request")
    val token = accountManager.getToken(currentServerUrl)
      ?: throw IllegalStateException("No authentication found for the url $currentServerUrl")
    return client.request {
      url("${cleanServerUrl(currentServerUrl)}${if (!endpoint.startsWith("/")) "/" else ""}$endpoint")
      header(HttpHeaders.Authorization, "Bearer $token")
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
}
