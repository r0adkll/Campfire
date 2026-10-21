// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import app.campfire.audioplayer.PlaybackFailedException
import app.campfire.audioplayer.PlaybackFailedException.Kind
import java.io.EOFException

/**
 * Whether [this] is how a streaming cache entry with a truncated stored length fails: the
 * extractor runs off the cached end of the file mid-sample (`EOFException`), or can't find the
 * container's index at all because it lies past that end.
 */
internal fun PlaybackException.mayBeCorruptCache(): Boolean = when (errorCode) {
  PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
  PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
  PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
  -> true
  PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> hasCause<EOFException>()
  else -> false
}

internal fun PlaybackException.asPlaybackFailure(): PlaybackFailedException {
  val httpStatus = findCause<InvalidResponseCodeException>()?.responseCode
  val kind = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_TIMEOUT,
    -> Kind.Network
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> when (httpStatus) {
      401, 403 -> Kind.Unauthorized
      404, 410 -> Kind.NotFound
      else -> Kind.Unknown
    }
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> Kind.NotFound
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
      if (hasCause<EOFException>()) Kind.UnsupportedFormat else Kind.Unknown
    in PARSING_ERROR_CODES, in DECODING_ERROR_CODES -> Kind.UnsupportedFormat
    else -> Kind.Unknown
  }
  val code = errorCodeName.removePrefix("ERROR_CODE_") +
    (httpStatus?.let { " (HTTP $it)" } ?: "")
  return PlaybackFailedException(kind = kind, code = code, message = message, cause = this)
}

// media3 groups its error codes by thousands: 3xxx parsing, 4xxx decoding
private val PARSING_ERROR_CODES = 3000..3999
private val DECODING_ERROR_CODES = 4000..4999

private inline fun <reified T : Throwable> Throwable.findCause(): T? =
  generateSequence(cause) { it.cause }.filterIsInstance<T>().firstOrNull()

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean = findCause<T>() != null
