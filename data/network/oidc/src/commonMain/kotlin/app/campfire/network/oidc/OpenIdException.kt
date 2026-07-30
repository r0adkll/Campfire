// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

sealed class OpenIdException(message: String?) : Exception(message) {

  class AuthCancelled(message: String? = null) : OpenIdException(message)
  class AuthFailure(message: String? = null) : OpenIdException(message)
}
