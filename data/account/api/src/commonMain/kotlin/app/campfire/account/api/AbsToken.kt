// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.account.api

data class AbsToken(
  val accessToken: String,
  val refreshToken: String?,
)
