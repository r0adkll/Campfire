// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackFailedException
import app.campfire.audioplayer.PlaybackFailedException.Kind
import platform.AVFoundation.AVErrorDecodeFailed
import platform.AVFoundation.AVErrorFailedToParse
import platform.AVFoundation.AVErrorFileFormatNotRecognized
import platform.AVFoundation.AVFoundationErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDataNotAllowed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorFileDoesNotExist
import platform.Foundation.NSURLErrorInternationalRoamingOff
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import platform.Foundation.NSURLErrorUserAuthenticationRequired
import platform.Foundation.NSURLErrorUserCancelledAuthentication
import platform.Foundation.NSUnderlyingErrorKey

/**
 * Translates an AVFoundation failure into a [PlaybackFailedException] the UI can name. The chain
 * of underlying errors is searched for the first one that says what happened. Against an ABS
 * server, an HTTP 401 surfaces as `NSURLErrorUserAuthenticationRequired` and a 404 as
 * `NSURLErrorFileDoesNotExist`, each wrapping an OSStatus.
 */
internal fun NSError.asPlaybackFailure(message: String): PlaybackFailedException {
  val chain = generateSequence(this) { it.userInfo[NSUnderlyingErrorKey] as? NSError }
    .take(MAX_UNDERLYING_DEPTH)
    .toList()
  val explained = chain.firstNotNullOfOrNull { error -> error.failureKind()?.let { error to it } }
  val (source, kind) = explained ?: (this to Kind.Unknown)
  return PlaybackFailedException(
    kind = kind,
    code = "${source.domain} ${source.code}",
    message = "$message (${source.domain} ${source.code}: ${source.localizedDescription})",
  )
}

private fun NSError.failureKind(): Kind? = when (domain) {
  NSURLErrorDomain -> when (code) {
    NSURLErrorTimedOut,
    NSURLErrorCannotFindHost,
    NSURLErrorCannotConnectToHost,
    NSURLErrorNetworkConnectionLost,
    NSURLErrorDNSLookupFailed,
    NSURLErrorNotConnectedToInternet,
    NSURLErrorInternationalRoamingOff,
    NSURLErrorDataNotAllowed,
    -> Kind.Network

    NSURLErrorUserCancelledAuthentication,
    NSURLErrorUserAuthenticationRequired,
    -> Kind.Unauthorized

    NSURLErrorFileDoesNotExist -> Kind.NotFound
    else -> null
  }

  AVFoundationErrorDomain -> when (code) {
    AVErrorFileFormatNotRecognized,
    AVErrorFailedToParse,
    AVErrorDecodeFailed,
    -> Kind.UnsupportedFormat

    else -> null
  }

  else -> null
}

private const val MAX_UNDERLYING_DEPTH = 5
