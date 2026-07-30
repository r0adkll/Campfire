// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

data class OpenIdAuthorization(
  val codeVerifier: String,
  val code: String,
  val state: String,
)
