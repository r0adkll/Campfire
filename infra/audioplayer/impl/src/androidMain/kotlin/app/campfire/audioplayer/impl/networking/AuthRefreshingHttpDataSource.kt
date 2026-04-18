package app.campfire.audioplayer.impl.networking

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.TransferListener
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.io.IOException
import kotlinx.coroutines.runBlocking

private const val HTTP_UNAUTHORIZED = 401

/**
 * Wraps an upstream HTTP [DataSource] to recover from `401` responses by piggybacking on Ktor's
 * `Auth.bearer` plugin. On a 401 we issue a `GET /api/ping` through the user-scoped [HttpClient];
 * Ktor's auth plugin sees the 401, runs its mutex-guarded `refreshTokens` flow, persists the new
 * token via `AccountManager`, and the original error is rethrown so the upper Loader retries —
 * by then the outer `ResolvingDataSource` will pull the fresh token.
 *
 * TODO: Replace with KtorDataSource when https://github.com/androidx/media/pull/3071 is available
 */
@UnstableApi
class AuthRefreshingHttpDataSource(
  private val inner: DataSource,
  private val userClient: HttpClient,
) : DataSource {

  override fun open(dataSpec: DataSpec): Long {
    return try {
      inner.open(dataSpec)
    } catch (e: InvalidResponseCodeException) {
      if (e.responseCode != HTTP_UNAUTHORIZED) throw e
      if (!pingForRefresh(dataSpec.uri)) {
        throw NonRetryableAuthException(e)
      }
      throw e
    }
  }

  private fun pingForRefresh(uri: Uri): Boolean {
    val scheme = uri.scheme ?: return false
    val authority = uri.authority ?: return false
    val pingUrl = "$scheme://$authority/api/me"
    return runBlocking {
      try {
        userClient.get(pingUrl).status.isSuccess()
      } catch (t: Throwable) {
        bark("AuthRefreshingDataSource", LogPriority.ERROR) {
          "Refresh ping failed: ${t.message}"
        }
        false
      }
    }
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
    inner.read(buffer, offset, length)

  override fun addTransferListener(transferListener: TransferListener) {
    inner.addTransferListener(transferListener)
  }

  override fun getUri(): Uri? = inner.uri

  override fun getResponseHeaders(): Map<String, List<String>> = inner.responseHeaders

  override fun close() = inner.close()

  class Factory(
    private val upstream: DataSource.Factory,
    private val userClient: HttpClient,
  ) : DataSource.Factory {
    override fun createDataSource(): DataSource = AuthRefreshingHttpDataSource(
      inner = upstream.createDataSource(),
      userClient = userClient,
    )
  }
}

class NonRetryableAuthException(cause: Throwable) : IOException(cause)
