package app.campfire.network

import app.campfire.network.di.ServerUrl
import app.campfire.network.models.NetworkModel
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.URLParserException
import io.ktor.http.isSuccess
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

internal suspend inline fun <reified T> trySendRequest(
  noinline responseMapper: suspend (HttpResponse) -> T = { it.body<T>() },
  context: CoroutineContext = Dispatchers.IO,
  crossinline request: suspend () -> HttpResponse,
): Result<T> = withContext(context) {
  try {
    val response = request()
    if (response.status.isSuccess()) {
      val originServerUrl = response.call.request.headers[HttpHeaders.ServerUrl]
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
      Result.failure(ApiException(response.status.value, "Network request failed"))
    }
  } catch (e: IOException) {
    e.printStackTrace()
    Result.failure(e)
  } catch (e: NoTransformationFoundException) {
    e.printStackTrace()
    Result.failure(e)
  } catch (e: IllegalArgumentException) {
    e.printStackTrace()
    Result.failure(e)
  } catch (e: URLParserException) {
    e.printStackTrace()
    Result.failure(e)
  } catch (t: Throwable) {
    Result.failure(t)
  }
}
