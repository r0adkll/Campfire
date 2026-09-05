// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.account.api.AccountManager
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
 * Downloads cover images for Now Playing with the user's bearer token. Covers are small and
 * change only when the item changes, so one fetch per URL with no persistent cache is enough.
 */
internal class HttpArtworkLoader(
  private val accountManager: AccountManager,
  private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
) : ArtworkLoader {

  override suspend fun load(url: String, userId: UserId): ByteArray? {
    val token = runCatching { accountManager.getToken(userId)?.accessToken }.getOrNull()
    return withContext(Dispatchers.IO) {
      val request = HttpRequest.newBuilder(URI(url))
        .timeout(Duration.ofSeconds(20))
        .apply { if (token != null) header("Authorization", "Bearer $token") }
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
