// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLParserException
import io.ktor.http.Url
import io.ktor.http.contentType

private const val ACCESS_TEAM_DOMAIN_SUFFIX = ".cloudflareaccess.com"
private const val ACCESS_AUTH_SCHEME = "Cloudflare-Access"
private const val SERVICE_TOKEN_CLIENT_ID_HEADER = "CF-Access-Client-Id"

/**
 * The Cloudflare Access challenge in this response, if Access answered it instead of the server.
 * Access challenges a request with a redirect to the team's login page, or, for applications set
 * to answer service clients directly, a 401/403. A 401/403 counts when it names Access in
 * `WWW-Authenticate`, or when it's a Cloudflare HTML page answering a request that carried a
 * service token, as strict service token authentication does. Every response through a proxied
 * Cloudflare hostname says `Server: cloudflare`, so that alone would also match its firewall and
 * bot challenge pages.
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

  if (status != HttpStatusCode.Unauthorized && status != HttpStatusCode.Forbidden) return null

  val namesAccess = headers.getAll(HttpHeaders.WWWAuthenticate).orEmpty().any { challenge ->
    challenge.trimStart().substringBefore(' ').equals(ACCESS_AUTH_SCHEME, ignoreCase = true)
  }
  val rejectsServiceToken = request.headers[SERVICE_TOKEN_CLIENT_ID_HEADER] != null &&
    headers[HttpHeaders.Server].equals("cloudflare", ignoreCase = true) &&
    contentType()?.match(ContentType.Text.Html) == true
  return if (namesAccess || rejectsServiceToken) CloudflareAccessException(teamDomain = null) else null
}
