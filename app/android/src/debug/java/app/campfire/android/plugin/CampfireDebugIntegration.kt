// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import app.campfire.android.plugin.playback.DebugArtworkLoader
import app.campfire.android.plugin.playback.SessionDebugCollector
import app.campfire.audioplayer.impl.AudioPlayerDebugHooks
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridges Campfire's [AudioPlayerDebugHooks] seam into the app-agnostic playback
 * Livewire plugin. The plugin only exposes media3-typed collection points; this
 * adapter is the Campfire-specific glue that feeds them.
 */
object CampfireAudioPlayerDebugHooks : AudioPlayerDebugHooks {

  override fun onExoPlayerCreated(exoPlayer: ExoPlayer) {
    exoPlayer.addAnalyticsListener(SessionDebugCollector.analyticsListener)
  }

  override fun onSessionCreated(session: MediaLibrarySession) {
    SessionDebugCollector.attachSession(session)
  }

  override fun onSessionReleased() {
    SessionDebugCollector.detachSession()
  }

  override fun onControllerConnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
    SessionDebugCollector.recordControllerConnected(session, controller)
  }

  override fun onCustomCommand(controller: MediaSession.ControllerInfo, action: String, args: Bundle) {
    SessionDebugCollector.recordCustomCommand(controller.packageName, action, args)
  }

  override fun onMediaButtonEvent(packageName: String, keyCode: Int?) {
    SessionDebugCollector.recordMediaButtonEvent(packageName, keyCode)
  }
}

/**
 * Coil-backed [DebugArtworkLoader] for the mock player — routes through the app's
 * ImageLoader so CoverContentProvider content:// uris and auth-requiring server
 * urls both work, then scales and JPEG-compresses for the wire.
 */
class CoilDebugArtworkLoader(private val context: Context) : DebugArtworkLoader {

  override suspend fun load(uri: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
      val result = SingletonImageLoader.get(context)
        .execute(ImageRequest.Builder(context).data(uri).build())
      val image = (result as? SuccessResult)?.image ?: return@withContext null
      val bitmap = image.toBitmap()
      val scaled = if (bitmap.width > MAX_ARTWORK_DIMENSION) {
        val ratio = MAX_ARTWORK_DIMENSION.toFloat() / bitmap.width
        Bitmap.createScaledBitmap(bitmap, MAX_ARTWORK_DIMENSION, (bitmap.height * ratio).toInt(), true)
      } else {
        bitmap
      }
      ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
      }
    }.getOrNull()
  }

  companion object {
    private const val MAX_ARTWORK_DIMENSION = 512
  }
}
