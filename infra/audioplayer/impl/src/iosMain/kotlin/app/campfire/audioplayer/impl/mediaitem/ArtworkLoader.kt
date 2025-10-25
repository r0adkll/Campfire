package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.di.AppScope
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.r0adkll.kimchi.annotations.ContributesBinding
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.MediaPlayer.MPMediaItemArtwork
import platform.UIKit.UIImage

interface ArtworkLoader {

  suspend fun load(url: String): MPMediaItemArtwork?
}

@ContributesBinding(AppScope::class)
@Inject
class NetworkArtworkLoader(
  private val httpClient: HttpClient,
  private val dispatcherProvider: DispatcherProvider,
) : ArtworkLoader {

  override suspend fun load(url: String): MPMediaItemArtwork? = withContext(dispatcherProvider.io) {
    val response = httpClient.get(url)
    if (response.status.isSuccess()) {
      try {
        val nsData = response.bodyAsBytes().toNSData()
        MPMediaItemArtwork(UIImage(nsData))
      } catch (e: Exception) {
        bark(LogPriority.ERROR, throwable = e) { "Error loading artwork" }
        null
      }
    } else {
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
