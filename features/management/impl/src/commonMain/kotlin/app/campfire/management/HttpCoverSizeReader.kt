// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.UserScope
import app.campfire.core.image.CoverUrls
import app.campfire.management.api.match.CoverSizeReader
import app.campfire.management.api.match.ImageSize
import app.campfire.network.di.UserClient
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

/**
 * Reads only as much of a cover as its size takes, then stops. The user's client sends the
 * account's credentials to the user's own server alone, so a source's cover host gets none.
 */
@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class)
@Inject
class HttpCoverSizeReader(
  @UserClient private val client: HttpClient,
  private val dispatchers: DispatcherProvider,
) : CoverSizeReader {

  override suspend fun sizeOf(url: String): Result<ImageSize> = try {
    Result.success(withContext(dispatchers.io) { read(CoverUrls.original(url)) })
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }

  private suspend fun read(url: String): ImageSize = client.prepareGet(url).execute { response ->
    check(response.status.isSuccess()) { "HTTP ${response.status.value} for the cover" }
    val channel = response.bodyAsChannel()
    var bytes = ByteArray(0)
    while (bytes.size < MaxHeaderBytes) {
      val chunk = channel.readRemaining(ChunkBytes).readByteArray()
      if (chunk.isEmpty()) break
      bytes += chunk
      ImageHeaders.size(bytes)?.let { return@execute it }
    }
    error("The cover's size isn't in its first ${bytes.size} bytes")
  }
}

private const val ChunkBytes = 16 * 1024L

/** Past this, a size isn't coming: covers put it in their first few kilobytes */
private const val MaxHeaderBytes = 256 * 1024
