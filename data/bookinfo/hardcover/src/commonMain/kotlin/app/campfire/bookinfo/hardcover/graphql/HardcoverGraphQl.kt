// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.hardcover.graphql

import app.campfire.bookinfo.hardcover.auth.HardcoverTokenStorage
import app.campfire.bookinfo.hardcover.di.HardcoverClient
import app.campfire.core.session.UserSession
import app.campfire.core.session.userId
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.tatarka.inject.annotations.Inject

internal const val HARDCOVER_GRAPHQL_ENDPOINT = "https://api.hardcover.app/v1/graphql"

/**
 * Minimal GraphQL executor for the Hardcover API. Owns the request envelope,
 * the `{data, errors}` response unwrap (GraphQL errors arrive with HTTP 200),
 * and the auth/rate-limit error taxonomy. The bearer token is read from
 * [HardcoverTokenStorage] per request — tokens have no refresh flow, so nothing
 * may cache them across a re-link.
 */
@Inject
class HardcoverGraphQl(
  @HardcoverClient private val client: HttpClient,
  private val tokenStorage: HardcoverTokenStorage,
  private val userSession: UserSession,
) {

  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  suspend fun <T> execute(
    document: String,
    variables: JsonObject = EmptyVariables,
    deserializer: DeserializationStrategy<T>,
  ): HardcoverResult<T> {
    val userId = userSession.userId ?: return HardcoverResult.NotLinked
    val token = tokenStorage.getToken(userId) ?: return HardcoverResult.NotLinked
    val result = executeWithToken(token, document, variables, deserializer)
    if (result is HardcoverResult.TokenInvalid) {
      tokenStorage.markInvalid(userId)
    }
    return result
  }

  /**
   * Executes with an explicit [token], bypassing storage — used to verify a
   * pasted token before persisting it.
   */
  suspend fun <T> executeWithToken(
    token: String,
    document: String,
    variables: JsonObject = EmptyVariables,
    deserializer: DeserializationStrategy<T>,
  ): HardcoverResult<T> {
    val response = try {
      client.post(HARDCOVER_GRAPHQL_ENDPOINT) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $token")
        setBody(
          buildJsonObject {
            put("query", document)
            put("variables", variables)
          }.toString(),
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      return HardcoverResult.NetworkFailure(e)
    }

    return when {
      response.status == HttpStatusCode.Unauthorized ||
        response.status == HttpStatusCode.Forbidden -> HardcoverResult.TokenInvalid

      response.status == HttpStatusCode.TooManyRequests ->
        HardcoverResult.RateLimited(parseRetryAfter(response.headers))

      !response.status.isSuccess() ->
        HardcoverResult.NetworkFailure(HardcoverHttpException(response.status.value))

      else -> parseEnvelope(response.bodyAsText(), deserializer)
    }
  }

  private fun <T> parseEnvelope(
    body: String,
    deserializer: DeserializationStrategy<T>,
  ): HardcoverResult<T> {
    return try {
      val envelope = json.parseToJsonElement(body).jsonObject
      val errors = envelope["errors"]?.jsonArray.orEmpty()
      if (errors.isNotEmpty()) {
        val messages = errors.mapNotNull { error ->
          error.jsonObject["message"]?.jsonPrimitive?.content
        }
        return HardcoverResult.GraphQlErrors(messages)
      }
      val data = envelope["data"]
        ?: return HardcoverResult.NetworkFailure(HardcoverGraphQlException(listOf("Missing data element")))
      HardcoverResult.Success(json.decodeFromJsonElement(deserializer, data))
    } catch (e: Exception) {
      HardcoverResult.NetworkFailure(e)
    }
  }

  companion object {
    val EmptyVariables = buildJsonObject { }
  }
}

/**
 * Resolves a retry delay from a 429 response, checking `Retry-After`, the IETF
 * draft `RateLimit` header, and the legacy `X-RateLimit-Reset` (epoch seconds).
 */
internal fun parseRetryAfter(
  headers: Headers,
  nowEpochSeconds: Long = Clock.System.now().epochSeconds,
): Duration? {
  headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.let { return it.seconds }

  headers["RateLimit"]?.let { value ->
    RATE_LIMIT_RESET_REGEX.find(value)?.groupValues?.getOrNull(2)?.toLongOrNull()?.let { return it.seconds }
  }

  headers["X-RateLimit-Reset"]?.toLongOrNull()?.let { reset ->
    // Some servers send epoch seconds, others send seconds-until-reset.
    return if (reset > EPOCH_SECONDS_THRESHOLD) (reset - nowEpochSeconds).coerceAtLeast(0).seconds else reset.seconds
  }

  return null
}

private val RATE_LIMIT_RESET_REGEX = "\\b(reset|t)=(\\d+)".toRegex()
private const val EPOCH_SECONDS_THRESHOLD = 1_000_000_000L
