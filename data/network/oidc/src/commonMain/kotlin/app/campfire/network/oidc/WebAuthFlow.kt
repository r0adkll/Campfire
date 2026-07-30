// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.oidc

import io.ktor.http.Url

/**
 * An interface for making authorization requests to AudioBookShelf's OpenID system via
 * a web browser.
 */
interface WebAuthFlow {
  suspend fun startWebFlow(requestUrl: Url, redirectUri: String): WebAuthFlowResult
}

sealed interface WebAuthFlowResult {
  data class Success(val responseUrl: Url?) : WebAuthFlowResult
  data object Cancelled : WebAuthFlowResult
}
