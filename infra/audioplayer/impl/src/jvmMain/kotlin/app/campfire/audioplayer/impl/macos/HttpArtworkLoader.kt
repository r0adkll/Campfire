// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.impl.ServerRequestHeaders
import app.campfire.core.logging.Cork
import app.campfire.core.model.UserId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads cover images for Now Playing with the user's server credentials. Covers are small and
 * change only when the item changes, so one fetch per URL with no persistent cache is enough.
 */
internal class HttpArtworkLoader(
  private val serverRequestHeaders: ServerRequestHeaders,
  private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : ArtworkLoader {

  override suspend fun load(url: String, userId: UserId): ByteArray? {
    val headers = runCatching { serverRequestHeaders.forUrl(userId, url) }.getOrDefault(emptyMap())
    return withContext(Dispatchers.IO) {
      val request = HttpRequest.newBuilder(URI(url))
        .timeout(Duration.ofSeconds(20))
        .apply { headers.forEach { (name, value) -> header(name, value) } }
        .GET()
        .build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
      if (response.statusCode() in 200..299 && response.body().isNotEmpty()) {
        response.body()
      } else {
        wbark { "Cover request failed: HTTP ${response.statusCode()}" }
        null
      }
    }
  }

  companion object : Cork {
    override val tag: String = "HttpArtworkLoader"
    override val enabled: Boolean = true
  }
}
