// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.plugins

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.util.AttributeKey

/**
 * Keeps credentials on the server they were meant for when a redirect leaves it.
 *
 * Ktor's `HttpRedirect` copies every header onto the next hop and only drops `Authorization` when
 * the authority changes, so an account's extra headers (reverse-proxy auth, Cloudflare Access
 * service tokens) would follow a 3xx to any host. `HttpRedirect` is installed ahead of every
 * plugin a client config adds, so its `Send` hook wraps this one, which therefore sees each hop.
 * Once a hop's host or port differs from the first request's, only [forwardableHeaders] go out.
 * A later hop back to the first host doesn't get them back: each hop starts from the one before.
 */
val RedirectHeaderGuard = createClientPlugin("RedirectHeaderGuard") {
  on(Send) { request ->
    val origin = request.attributes.computeIfAbsent(OriginAuthorityKey) { request.url.authorityKey() }
    if (request.url.authorityKey() != origin) {
      request.headers.names()
        .filterNot { it.lowercase() in forwardableHeaders }
        .forEach(request.headers::remove)
    }
    proceed(request)
  }
}

private val OriginAuthorityKey = AttributeKey<String>("RedirectHeaderGuardOrigin")

/** Host and explicit port, as `HttpRedirect` compares them, so an http to https upgrade is the same server. */
private fun URLBuilder.authorityKey(): String = "${host.lowercase()}:$port"

/** Headers that describe the request rather than who is making it. */
private val forwardableHeaders = listOf(
  HttpHeaders.Accept,
  HttpHeaders.AcceptCharset,
  HttpHeaders.AcceptEncoding,
  HttpHeaders.AcceptLanguage,
  HttpHeaders.CacheControl,
  HttpHeaders.Connection,
  HttpHeaders.ContentLength,
  HttpHeaders.ContentType,
  HttpHeaders.IfMatch,
  HttpHeaders.IfModifiedSince,
  HttpHeaders.IfNoneMatch,
  HttpHeaders.IfRange,
  HttpHeaders.IfUnmodifiedSince,
  HttpHeaders.Pragma,
  HttpHeaders.Range,
  HttpHeaders.SecWebSocketExtensions,
  HttpHeaders.SecWebSocketKey,
  HttpHeaders.SecWebSocketProtocol,
  HttpHeaders.SecWebSocketVersion,
  HttpHeaders.Upgrade,
  HttpHeaders.UserAgent,
).mapTo(mutableSetOf()) { it.lowercase() }
