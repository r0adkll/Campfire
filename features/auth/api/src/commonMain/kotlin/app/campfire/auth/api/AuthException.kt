// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.api

/**
 * Typed authentication failures returned by [AuthRepository.authenticate] so that
 * callers can present an accurate error message instead of guessing from raw
 * network/serialization exceptions.
 */
sealed class AuthException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause) {

  /** The server rejected the provided credentials. */
  class InvalidCredentials(cause: Throwable? = null) :
    AuthException("The server rejected the provided credentials", cause)

  /** Login succeeded, but the account has no accessible libraries on the server. */
  class NoAccessibleLibraries :
    AuthException("The account has no accessible libraries on the server")

  /** The connection to the server failed. */
  class Network(cause: Throwable) :
    AuthException("Network failure during authentication", cause)

  /** The server responded in a way the app couldn't understand. */
  class UnexpectedResponse(message: String, cause: Throwable? = null) :
    AuthException(message, cause)
}
