// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.logging

import app.campfire.core.permission.extractUrlHost
import co.touchlab.stately.collections.ConcurrentMutableMap

/**
 * Scrubs user server URLs (and any other URL) out of log messages before they reach
 * any [Heartwood.Bark], keeping paths intact for diagnostics. Two layers:
 *
 * 1. A generic pass that replaces the authority of any `http(s)://` / `ws(s)://` URL.
 * 2. An exact-match pass over values registered via [registerServerUrl] — this also
 *    catches *bare* hosts (e.g. `UnknownHostException: abs.myserver.net`) that the
 *    generic pass can't recognize.
 *
 * Registered values map to stable hash-derived tokens so multi-server users can still
 * be correlated across log lines without exposing the underlying host.
 */
object LogRedaction {

  /**
   * Redaction is on by default so release builds are safe even if no initializer runs;
   * debug entry points opt out to keep local logs readable.
   */
  var enabled: Boolean = true

  private val replacements = ConcurrentMutableMap<String, String>()

  private val genericUrlRegex = Regex("\\b(https?|wss?)://[^/\\s\"'<>\\\\]+", RegexOption.IGNORE_CASE)

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
