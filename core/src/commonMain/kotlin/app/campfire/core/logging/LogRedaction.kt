// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.logging

import app.campfire.core.permission.extractUrlHost
import co.touchlab.stately.collections.ConcurrentMutableMap

/**
 * Scrubs user server URLs (and any other URL) and credentials out of log messages before they
 * reach any [Heartwood.Bark], keeping paths intact for diagnostics. Three layers:
 *
 * 1. Credentials: values registered via [registerSecret] (e.g. custom header values), bearer
 *    tokens, JWTs, and token-like query parameters (`?token=`, `code=`, …).
 * 2. A generic pass that replaces the authority of any `http(s)://` / `ws(s)://` URL.
 * 3. An exact-match pass over values registered via [registerServerUrl] — this also
 *    catches *bare* hosts (e.g. `UnknownHostException: abs.myserver.net`) that the
 *    generic pass can't recognize.
 *
 * Registered server values map to stable hash-derived tokens so multi-server users can still
 * be correlated across log lines without exposing the underlying host.
 */
object LogRedaction {

  /**
   * Redaction is on by default so release builds are safe even if no initializer runs;
   * debug entry points opt out to keep local logs readable.
   */
  var enabled: Boolean = true

  /** Shorter secrets would scrub unrelated text out of logs; they're too weak to matter anyway. */
  private const val MIN_SECRET_LENGTH = 8

  private val replacements = ConcurrentMutableMap<String, String>()
  private val secrets = ConcurrentMutableMap<String, Unit>()

  private val genericUrlRegex = Regex("\\b(https?|wss?)://[^/\\s\"'<>\\\\]+", RegexOption.IGNORE_CASE)

  private val sensitiveQueryRegex = Regex(
    "([?&;](?:token|access_token|refresh_token|id_token|code|code_verifier|client_secret)=)[^&#\\s\"'<>]+",
    RegexOption.IGNORE_CASE,
  )

  private val bearerRegex = Regex("\\b(Bearer)\\s+[A-Za-z0-9._~+/=-]+", RegexOption.IGNORE_CASE)

  private val jwtRegex = Regex("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*")

  /**
   * Registers a credential (e.g. a custom connection header value) to be replaced wherever it
   * appears in a log message.
   */
  fun registerSecret(secret: String) {
    val value = secret.trim()
    if (value.length < MIN_SECRET_LENGTH) return
    secrets[value] = Unit
  }

  fun registerServerUrl(serverUrl: String) {
    val url = serverUrl.trim().trimEnd('/')
    if (url.isEmpty()) return

    replacements[url] = "<server#${token(url)}>"

    extractUrlHost(url)?.let { host ->
      replacements[host] = "<host#${token(host)}>"
      val lowercase = host.lowercase()
      if (lowercase != host) {
        replacements[lowercase] = "<host#${token(host)}>"
      }
    }
  }

  fun redact(message: String): String {
    if (!enabled) return message

    var result = message
    // Longest-first so a secret containing another is consumed whole.
    secrets.keys
      .sortedByDescending { it.length }
      .forEach { secret -> result = result.replace(secret, "<secret>") }

    result = bearerRegex.replace(result) { match -> "${match.groupValues[1]} <redacted>" }
    result = jwtRegex.replace(result, "<jwt>")
    result = sensitiveQueryRegex.replace(result) { match -> "${match.groupValues[1]}<redacted>" }

    // Longest-first so a full URL is consumed before its bare host.
    replacements.entries
      .sortedByDescending { it.key.length }
      .forEach { (value, placeholder) ->
        result = result.replace(value, placeholder)
      }

    result = genericUrlRegex.replace(result) { match ->
      "${match.groupValues[1]}://<redacted>"
    }

    return result
  }

  fun clear() {
    replacements.clear()
    secrets.clear()
  }

  private fun token(value: String): String =
    value.lowercase().hashCode().toUInt().toString(16).padStart(8, '0').take(6)
}

/**
 * A version of a URL that is safe to log: the scheme and authority (which identify the
 * user's private server) are dropped, keeping only the path for diagnostics. Query and
 * fragment are dropped too since they can carry tokens.
 */
val String.loggableUrl: String
  get() {
    val schemeIdx = indexOf("://")
    val afterScheme = if (schemeIdx >= 0) schemeIdx + 3 else 0
    val pathStart = indexOf('/', afterScheme)
    val path = if (pathStart >= 0) {
      substring(pathStart).substringBefore('?').substringBefore('#')
    } else {
      ""
    }
    return "<server>$path"
  }
