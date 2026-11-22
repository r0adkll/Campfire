// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.image.CoverUrls
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.network.di.UserClient
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.MediaPlayer.MPMediaItemArtwork
import platform.UIKit.UIImage

interface ArtworkLoader {

  /** The cover at [url] as Now Playing artwork, or null when it can't be fetched or decoded. */
  suspend fun load(url: String): MPMediaItemArtwork?
}

@ContributesBinding(AppScope::class)
@Inject
class NetworkArtworkLoader(
  @UserClient private val httpClient: HttpClient,
  private val dispatcherProvider: DispatcherProvider,
) : ArtworkLoader {

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun load(url: String): MPMediaItemArtwork? = withContext(dispatcherProvider.io) {
    // Artwork is decoration: an unreachable server or a bad image must never fail playback
    try {
      val response = httpClient.get(CoverUrls.sized(url, CoverUrls.ARTWORK_WIDTH))
      if (!response.status.isSuccess()) return@withContext null
      val image = UIImage.imageWithData(response.bodyAsBytes().toNSData()) ?: return@withContext null
      MPMediaItemArtwork(boundsSize = image.size) { _ -> image }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      bark(LogPriority.WARN, throwable = e) { "Unable to load Now Playing artwork" }
      null
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.dataWithBytes(
      bytes = allocArrayOf(this@toNSData),
      length = this@toNSData.size.toULong(),
    )
  }
}
