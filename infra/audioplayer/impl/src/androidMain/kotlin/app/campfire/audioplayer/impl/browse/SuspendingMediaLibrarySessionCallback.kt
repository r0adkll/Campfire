package app.campfire.audioplayer.impl.browse

import android.annotation.SuppressLint
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future

/**
 * ListenableFuture to Coroutines adapting base class for MediaLibrarySession.Callback.
 *
 * Each metho is implemented like for like,
 */
abstract class SuspendingMediaLibrarySessionCallback(
  private val serviceScope: CoroutineScope,
) : MediaLibrarySession.Callback {

  @SuppressLint("UnsafeOptInUsageError")
  override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    return serviceScope.future {
      try {
        onGetLibraryRootInternal(session, browser, params)
      } catch (e: Exception) {
        bark(LogPriority.ERROR, throwable = e) { "onGetLibraryRoot: ${browser.uid}" }
        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      }
    }
  }

  protected abstract suspend fun onGetLibraryRootInternal(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: MediaLibraryService.LibraryParams?,
  ): LibraryResult<MediaItem>

  @SuppressLint("UnsafeOptInUsageError")
  override fun onGetItem(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String,
  ): ListenableFuture<LibraryResult<MediaItem>> {
    return serviceScope.future {
      try {
        onGetItemInternal(session, browser, mediaId)
      } catch (e: Exception) {
        bark(LogPriority.ERROR, throwable = e) { "onGetItem: ${browser.uid}" }
        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      }
    }
  }

  protected abstract suspend fun onGetItemInternal(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    mediaId: String,
  ): LibraryResult<MediaItem>

  override fun onAddMediaItems(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
  ): ListenableFuture<MutableList<MediaItem>> {
    return serviceScope.future {
      onAddMediaItemsInternal(mediaSession, controller, mediaItems)
    }
  }

  /**
   * default implementation of onAddMediaItems that sets the URI from the requestMetadata
   * if present.
   */
  protected open suspend fun onAddMediaItemsInternal(
    mediaSession: MediaSession,
    controller: MediaSession.ControllerInfo,
    mediaItems: MutableList<MediaItem>,
  ): MutableList<MediaItem> {
    return mediaItems.map {
      if (it.requestMetadata.mediaUri != null) {
        it.buildUpon()
          .setUri(it.requestMetadata.mediaUri)
          .build()
      } else {
        it
      }
    }.toMutableList()
  }

  @SuppressLint("UnsafeOptInUsageError")
  override fun onGetChildren(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?,
  ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    return serviceScope.future {
      try {
        onGetChildrenInternal(session, browser, parentId, page, pageSize, params)
      } catch (e: Exception) {
        bark(LogPriority.ERROR, throwable = e) { "onGetChildren: ${browser.uid}" }
        LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
      }
    }
  }

  protected abstract suspend fun onGetChildrenInternal(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: MediaLibraryService.LibraryParams?,
  ): LibraryResult<ImmutableList<MediaItem>>
}
