// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.image

import app.campfire.core.image.CoverUrls
import app.campfire.covers.api.OfflineCover
import app.campfire.covers.api.OfflineCovers
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import okio.Path.Companion.toPath

/**
 * Shows the cover kept for a downloaded item ([OfflineCovers]) in place of the server's, so it
 * shows offline and after the OS clears the image cache. The kept cover is used straight away
 * while it's the one the request asks for, and otherwise only when the server can't be reached.
 *
 * Goes ahead of [CoverSizingInterceptor], which is left to size the request when it goes to the
 * server.
 */
class OfflineCoverInterceptor(
  private val offlineCovers: OfflineCovers,
) : Interceptor {

  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val url = chain.request.data as? String ?: return chain.proceed()
    val itemId = CoverUrls.itemId(url) ?: return chain.proceed()
    val cover = offlineCovers.get(itemId) ?: return chain.proceed()

    if (cover.isCurrentFor(url)) return chain.proceedWith(cover)
    val result = chain.proceed()
    return if (result is ErrorResult) chain.proceedWith(cover) else result
  }

  private suspend fun Interceptor.Chain.proceedWith(cover: OfflineCover): ImageResult = withRequest(
    request.newBuilder()
      .data(cover.path.toPath())
      .build(),
  ).proceed()
}
