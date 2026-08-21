// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.content

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import app.campfire.account.api.UrlHydrator
import app.campfire.core.di.ComponentHolder
import app.campfire.core.image.CoverUrls
import app.campfire.core.logging.Corked
import app.campfire.core.logging.loggableUrl
import coil3.imageLoader
import coil3.request.ImageRequest
import java.io.FileNotFoundException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class CoverContentProvider : ContentProvider() {

  override fun onCreate(): Boolean = true

  override fun getType(uri: Uri): String = "image/*"

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    if (mode != "r") {
      throw SecurityException("CoverContentProvider only supports read mode, got '$mode' for $uri")
    }

    val ctx = context
      ?: throw FileNotFoundException("ContentProvider has no context")
    // The app's image loader rewrites server cover URLs to request a sized rendition, and that rewritten
    // URL is the disk cache key — so look up (and warm) the same rendition it would produce.
    val url = resolveRemoteUrl(uri)
      ?.let { CoverUrls.sized(it, CoverUrls.ARTWORK_WIDTH) }
      ?: throw FileNotFoundException("Unable to resolve remote URL for $uri")

    val loader = ctx.imageLoader
    val cache = loader.diskCache
      ?: throw FileNotFoundException("Coil disk cache is not configured")

    cache.openSnapshot(url)?.use { snapshot ->
      dbark { "Cover snapshot for ${url.loggableUrl} found" }
      return ParcelFileDescriptor.open(snapshot.data.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    return runBlocking {
      dbark { "Requesting cover snapshot for ${url.loggableUrl}" }
      val warmed = withTimeoutOrNull(CoverFetchTimeout) {
        loader.execute(
          ImageRequest.Builder(ctx)
            .data(url)
            .size(CoverUrls.ARTWORK_WIDTH)
            .build(),
        )
      }
      if (warmed == null) {
        wbark { "Timeout warming cover for $uri" }
        throw FileNotFoundException("Timed out fetching cover for $uri")
      }
      cache.openSnapshot(url)?.use { snapshot ->
        ParcelFileDescriptor.open(snapshot.data.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
      } ?: throw FileNotFoundException("Cover not present in cache after fetch for $uri")
    }
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0

  override fun delete(
    uri: Uri,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = 0

  private fun resolveRemoteUrl(uri: Uri): String? {
    val hydrator = ComponentHolder.maybeComponent<CoverContentUserComponent>()?.urlHydrator
      ?: return null
    val type = uri.pathSegments.getOrNull(0) ?: return null
    val id = uri.pathSegments.getOrNull(1) ?: return null
    return runCatching { hydrator.hydrate(type, id) }.getOrNull()
  }

  private fun UrlHydrator.hydrate(type: String, id: String): String? = when (type) {
    PATH_ITEMS -> hydrateLibraryItem(id)
    PATH_AUTHORS -> hydrateAuthor(id)
    else -> null
  }

  private companion object : Corked("CoverContentProvider") {
    val CoverFetchTimeout = 8.seconds
  }
}
