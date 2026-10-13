// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLParserException
import io.ktor.http.Url
import io.ktor.http.contentType

private const val ACCESS_TEAM_DOMAIN_SUFFIX = ".cloudflareaccess.com"

/**
 * The Cloudflare Access challenge in this response, if Access answered it instead of the server.
 * Access challenges a request with a redirect to the team's login page, or, for applications set
 * to answer service clients directly, a 401/403 HTML page from Cloudflare.
 *
 * Only meaningful for a response read without following redirects.
 */
internal fun HttpResponse.cloudflareAccessChallenge(): CloudflareAccessException? {
  if (status.value in 300..399) {
    val location = headers[HttpHeaders.Location] ?: return null
    val host = try {
      Url(location).host
    } catch (e: URLParserException) {
      return null
    }
    return if (host.endsWith(ACCESS_TEAM_DOMAIN_SUFFIX, ignoreCase = true)) {
      CloudflareAccessException(teamDomain = host.lowercase())
    } else {
      null
    }
  }

  val isAccessDenial = (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) &&
    headers[HttpHeaders.Server].equals("cloudflare", ignoreCase = true) &&
    contentType()?.match(ContentType.Text.Html) == true
  return if (isAccessDenial) CloudflareAccessException(teamDomain = null) else null
}
