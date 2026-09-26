// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

class ApiException(
  val statusCode: Int,
  val apiMessage: String = "",
) : Exception("API Error [$statusCode]: $apiMessage")

class AuthorizationException : Exception("Not valid login configuration found")

/**
 * Whether this failure is the server reporting that the requested resource doesn't exist.
 */
val Throwable.isNotFound: Boolean
  get() = this is ApiException && statusCode == 404
