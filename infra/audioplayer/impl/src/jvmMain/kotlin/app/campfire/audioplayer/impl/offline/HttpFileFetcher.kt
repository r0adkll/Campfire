// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.offline

import app.campfire.account.api.AccountManager
import app.campfire.account.api.TokenRefresher
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Streams one remote file into a `.part` file, resuming from whatever a previous attempt left
 * behind via an HTTP `Range` request.
 */
internal fun interface FileFetcher {

  /**
   * Download [request] into [partFile], appending to its current contents when the server honors
   * a range request. Returns once the full body is on disk; throws on failure or cancellation,
   * leaving [partFile] in place for a later resume.
   *
   * @param onProgress called with (bytes on disk, total bytes or <= 0 when unknown)
   */
  suspend fun fetch(request: FetchRequest, partFile: File, onProgress: (Long, Long) -> Unit)
}

internal data class FetchRequest(
  val url: String,
  val userId: String,
  val serverUrl: String,
  val expectedBytes: Long,
)

/** A failure that retrying won't fix — a rejected credential, a missing file, a denied permission. */
internal class PermanentFetchException(message: String) : IOException(message)

/**
 * [FileFetcher] over the [app.campfire.network.di.DownloadClient], which streams bodies without
 * buffering them and gives up on a transfer that stops receiving data. That client has no auth
 * plugin, so credentials are attached here to match the user client's (bearer token plus the
 * account's extra headers); a `401` goes through [TokenRefresher], which lets the user client —
 * the only refresh authority — rotate the token, then retries once.
 */
internal class HttpFileFetcher(
  private val client: HttpClient,
  private val accountManager: AccountManager,
  private val tokenRefresher: TokenRefresher,
) : FileFetcher {

  override suspend fun fetch(request: FetchRequest, partFile: File, onProgress: (Long, Long) -> Unit) {
    val token = accountManager.getToken(request.userId)?.accessToken
    if (fetch(request, token, partFile, onProgress)) return

    val refreshed = tokenRefresher.refresh(request.userId, request.serverUrl, token)?.accessToken
      ?: throw PermanentFetchException("Download rejected as unauthorized and the token could not be refreshed")
    if (!fetch(request, refreshed, partFile, onProgress)) {
      throw PermanentFetchException("Download rejected as unauthorized after a token refresh")
    }
  }

  /** Returns false when the server rejected [token] as unauthorized, true once the file is on disk. */
  private suspend fun fetch(
    request: FetchRequest,
    token: String?,
    partFile: File,
    onProgress: (Long, Long) -> Unit,
  ): Boolean = withContext(Dispatchers.IO) {
    val offset = partFile.length()
    val extraHeaders = accountManager.getExtraHeaders(request.userId).orEmpty()
    client.prepareGet(request.url) {
      if (!token.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
      extraHeaders.forEach { (name, value) -> header(name, value) }
      if (offset > 0) header(HttpHeaders.Range, "bytes=$offset-")
    }.execute { response ->
      when (val status = response.status) {
        HttpStatusCode.PartialContent -> {
          val total = response.contentRangeTotal() ?: response.contentLength()?.plus(offset) ?: request.expectedBytes
          copy(response.bodyAsChannel(), partFile, append = true, total, onProgress)
        }
        HttpStatusCode.OK -> {
          // A server that ignores Range sends the whole file again
          val total = response.contentLength() ?: request.expectedBytes
          copy(response.bodyAsChannel(), partFile, append = false, total, onProgress)
        }
        HttpStatusCode.RequestedRangeNotSatisfiable -> {
          // The part file already holds every byte (the previous attempt died before the rename),
          // or it's longer than the file now on the server — start that one over.
          val total = response.contentRangeTotal() ?: request.expectedBytes
          if (total <= 0 || offset != total) {
            partFile.delete()
            throw IOException("Partial download no longer matches the remote file")
          }
          onProgress(total, total)
        }
        HttpStatusCode.Unauthorized -> return@execute false
        HttpStatusCode.Forbidden, HttpStatusCode.NotFound -> throw PermanentFetchException("HTTP ${status.value}")
        else -> throw IOException("HTTP ${status.value}")
      }
      true
    }
  }

  private suspend fun copy(
    body: ByteReadChannel,
    partFile: File,
    append: Boolean,
    total: Long,
    onProgress: (Long, Long) -> Unit,
  ) {
    FileOutputStream(partFile, append).use { out ->
      var written = if (append) partFile.length() else 0L
      onProgress(written, total)
      val buffer = ByteArray(BUFFER_SIZE)
      while (true) {
        val read = body.readAvailable(buffer)
        if (read < 0) break
        currentCoroutineContext().ensureActive()
        out.write(buffer, 0, read)
        written += read
        onProgress(written, total)
      }
      if (total > 0 && written != total) {
        throw IOException("Download ended at $written of $total bytes")
      }
    }
  }

  /** The `N` in `Content-Range: bytes a-b/N` or `bytes *\/N`. */
  private fun HttpResponse.contentRangeTotal(): Long? =
    headers[HttpHeaders.ContentRange]?.substringAfterLast('/')?.toLongOrNull()

  private companion object {
    const val BUFFER_SIZE = 64 * 1024
  }
}
