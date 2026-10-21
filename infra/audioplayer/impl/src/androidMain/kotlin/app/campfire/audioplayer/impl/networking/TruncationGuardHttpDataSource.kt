// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.networking

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import java.io.EOFException

/**
 * Fails a response that ends before its declared length instead of reporting a clean end of input.
 *
 * `KtorDataSource` returns `RESULT_END_OF_INPUT` whenever the response channel closes, even with
 * bytes of a known `Content-Length` still outstanding — and Ktor's OkHttp engine closes that
 * channel normally when the call's context goes inactive or an HTTP/2 stream ends early. A
 * `CacheDataSource` writing that response takes the early end as the true end of the file and
 * persists the truncated length in the cache's metadata, so every later play stops at the same
 * point (`EOFException` mid-file, or no `moov` box for an m4b) until the cache is cleared.
 * Throwing here keeps the cache's length unset and lets the load error policy retry the read.
 */
@UnstableApi
class TruncationGuardHttpDataSource(
  private val upstream: HttpDataSource,
) : HttpDataSource by upstream {

  private var dataSpec: DataSpec? = null
  private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()

  override fun open(dataSpec: DataSpec): Long {
    this.dataSpec = dataSpec
    return upstream.open(dataSpec).also { bytesRemaining = it }
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    val read = upstream.read(buffer, offset, length)
    if (read == C.RESULT_END_OF_INPUT) {
      if (bytesRemaining > 0) {
        throw HttpDataSourceException(
          EOFException("Response ended with $bytesRemaining bytes left to read"),
          checkNotNull(dataSpec),
          PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
          HttpDataSourceException.TYPE_READ,
        )
      }
    } else if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
      bytesRemaining -= read
    }
    return read
  }

  override fun close() {
    dataSpec = null
    bytesRemaining = C.LENGTH_UNSET.toLong()
    upstream.close()
  }

  class Factory(
    private val upstream: HttpDataSource.Factory,
  ) : HttpDataSource.Factory by upstream {
    override fun createDataSource(): HttpDataSource =
      TruncationGuardHttpDataSource(upstream.createDataSource())
  }
}
