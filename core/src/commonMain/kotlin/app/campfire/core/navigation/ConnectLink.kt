// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.navigation

/**
 * A server and Cloudflare Access service token handed to Campfire by a setup link, e.g. from a
 * server's member page as a link or a QR code:
 *
 * `https://thescavengers.software/campfire/connect#v=1&server=https%3A%2F%2Fabs.example.com&name=Home&auth=openid&id=…&secret=…`
 *
 * The fields ride in the URL fragment, so they never reach the website, form-encoded the way
 * `URLSearchParams.toString()` writes them. Anyone can craft one of these links, so nothing in it
 * is applied until the user confirms it.
 *
 * @property serverUrl the server's https base URL, without a trailing slash
 * @property serverName the name the link gives the server, if any
 * @property signInWithOpenId whether to go straight on to OpenID sign-in once the server is saved
 */
data class ConnectLink(
  val serverUrl: String,
  val serverName: String?,
  val clientId: String,
  val clientSecret: String,
  val signInWithOpenId: Boolean,
) {

  /** The host (and port, if any) the service token will be sent to. */
  val serverHost: String
    get() = serverUrl.removePrefix(HTTPS).substringBefore('/')

  // Links get logged with the deep link that carries them; keep the token out of it.
  override fun toString(): String =
    "ConnectLink(serverHost=$serverHost, serverName=$serverName, clientId=<redacted>, " +
      "clientSecret=<redacted>, signInWithOpenId=$signInWithOpenId)"

  companion object {
    /** The App Link these arrive on. */
    const val HOST = "thescavengers.software"
    const val PATH = "/campfire/connect"

    /**
     * The custom-scheme form Chrome delivers from the Android fallback link
     * `intent://connect?…#Intent;scheme=campfireaudiobooks;package=app.campfire.android;end`. An
     * intent URL's fragment is taken by `#Intent;…`, so here the fields ride in the query.
     */
    const val SCHEME = "campfireaudiobooks"
    const val SCHEME_HOST = "connect"

    /** The link format this version of Campfire understands. */
    const val VERSION = "1"

    internal const val KEY_VERSION = "v"
    internal const val KEY_SERVER = "server"
    internal const val KEY_NAME = "name"
    internal const val KEY_AUTH = "auth"
    internal const val KEY_CLIENT_ID = "id"
    internal const val KEY_CLIENT_SECRET = "secret"
    internal const val AUTH_OPENID = "openid"

    private const val HTTPS = "https://"
    private const val MAX_NAME_LENGTH = 64
    private const val MAX_PORT = 65535
    private const val MAX_LABEL_LENGTH = 63

    private val knownKeys = setOf(KEY_VERSION, KEY_SERVER, KEY_NAME, KEY_AUTH, KEY_CLIENT_ID, KEY_CLIENT_SECRET)
    private val hostLabel = Regex("[a-z0-9]([a-z0-9-]*[a-z0-9])?")
    private val ipv6Literal = Regex("\\[[0-9a-f:.]+]")
    private val pathChars = Regex("[A-Za-z0-9._~!$'()*+,;=:%/-]*")

    /**
     * Parse a whole setup link, as it arrives from an intent or a scanned QR code, or `null` when
     * [url] isn't one. Two forms are setup links, matched exactly apart from the case of the
     * scheme and host and a trailing slash:
     *
     * - `https://thescavengers.software/campfire/connect#<fields>`, the App Link, with no query
     * - `campfireaudiobooks://connect?<fields>`, the Android fallback, with no fragment
     *
     * Anything else (another host, a port, user info, a different path) is not a setup link. A
     * setup link without its fields is one, but [ConnectLinkResult.Invalid].
     */
    fun parseUrl(url: String): ConnectLinkResult? {
      val text = url.trim()
      val schemeEnd = text.indexOf("://")
      if (schemeEnd < 0) return null
      val scheme = text.substring(0, schemeEnd)
      val rest = text.substring(schemeEnd + 3)

      return when {
        scheme.equals("https", ignoreCase = true) -> {
          val target = rest.substringBefore('#')
          if ('?' in target || !target.isLocation(HOST, PATH)) return null
          parse(rest.substringAfter('#', missingDelimiterValue = ""))
        }

        scheme.equals(SCHEME, ignoreCase = true) -> {
          val target = rest.substringBefore('?')
          if ('#' in rest || !target.isLocation(SCHEME_HOST, path = "")) return null
          parse(rest.substringAfter('?', missingDelimiterValue = ""))
        }

        else -> null
      }
    }

    /** Whether this `host/path` is exactly [host] (any case) followed by [path] or `$path/`. */
    private fun String.isLocation(host: String, path: String): Boolean {
      val authority = substringBefore('/')
      val rawPath = substring(authority.length)
      return authority.equals(host, ignoreCase = true) && (rawPath == path || rawPath == "$path/")
    }

    /**
     * Parse a setup link's [fragment] (the part after `#`). Unknown keys are ignored; values are
     * trimmed.
     */
    fun parse(fragment: String): ConnectLinkResult {
      val fields = decodeForm(fragment.removePrefix("#"))
        ?: return ConnectLinkResult.Invalid(ConnectLinkResult.Reason.Malformed)

      // A newer format may mean anything, so the version is checked before any other field
      val version = fields[KEY_VERSION] ?: return ConnectLinkResult.Invalid(ConnectLinkResult.Reason.Malformed)
      if (version != VERSION) return ConnectLinkResult.UnsupportedVersion

      val serverUrl = fields[KEY_SERVER]?.let(::normalizeServerUrl)
        ?: return ConnectLinkResult.Invalid(ConnectLinkResult.Reason.InvalidServer)

      val clientId = fields[KEY_CLIENT_ID]?.takeIf(::isHeaderToken)
      val clientSecret = fields[KEY_CLIENT_SECRET]?.takeIf(::isHeaderToken)
      if (clientId == null || clientSecret == null) {
        return ConnectLinkResult.Invalid(ConnectLinkResult.Reason.InvalidServiceToken)
      }

      val serverName = fields[KEY_NAME]
        ?.filterNot { it.isISOControl() }
        ?.trim()
        ?.take(MAX_NAME_LENGTH)
        ?.trim()
        ?.ifEmpty { null }

      return ConnectLinkResult.Valid(
        ConnectLink(
          serverUrl = serverUrl,
          serverName = serverName,
          clientId = clientId,
          clientSecret = clientSecret,
          signInWithOpenId = fields[KEY_AUTH].equals(AUTH_OPENID, ignoreCase = true),
        ),
      )
    }

    /**
     * The known fields of an `application/x-www-form-urlencoded` string, trimmed, or `null` when
     * it can't be decoded or repeats a known field.
     */
    private fun decodeForm(form: String): Map<String, String>? {
      val fields = mutableMapOf<String, String>()
      for (pair in form.split('&')) {
        if (pair.isEmpty()) continue
        val key = decodeFormComponent(pair.substringBefore('=')) ?: return null
        val value = decodeFormComponent(pair.substringAfter('=', missingDelimiterValue = "")) ?: return null
        if (key !in knownKeys) continue
        // Two values for one field leave it ambiguous which one the user would be agreeing to
        if (fields.put(key, value.trim()) != null) return null
      }
      return fields
    }

    /** Decodes `+` as a space and `%XX` as UTF-8 bytes. `null` for a bad escape or bad UTF-8. */
    private fun decodeFormComponent(component: String): String? {
      val bytes = ArrayList<Byte>(component.length)
      val literal = StringBuilder()
      fun flushLiteral() {
        if (literal.isNotEmpty()) {
          literal.toString().encodeToByteArray().forEach(bytes::add)
          literal.clear()
        }
      }

      var i = 0
      while (i < component.length) {
        when (val c = component[i]) {
          '+' -> literal.append(' ')
          '%' -> {
            val hex = component.substring(i + 1, minOf(i + 3, component.length))
            if (hex.length != 2 || !hex.all { it.isHexDigit() }) return null
            flushLiteral()
            bytes.add(hex.toInt(16).toByte())
            i += 2
          }
          else -> literal.append(c)
        }
        i++
      }
      flushLiteral()

      return try {
        bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
      } catch (_: CharacterCodingException) {
        null
      }
    }

    /**
     * [value] as an https base URL with a lowercase host and no trailing slash, or `null` unless it
     * is one. User info, queries and fragments are refused: `https://trusted.example@evil.example`
     * would send the token somewhere other than where it seems to.
     */
    private fun normalizeServerUrl(value: String): String? {
      if (!value.startsWith(HTTPS, ignoreCase = true)) return null
      val rest = value.substring(HTTPS.length)
      val rawAuthority = rest.substringBefore('/')
      val path = rest.substring(rawAuthority.length).trimEnd('/')
      val authority = rawAuthority.lowercase()
      if (!path.matches(pathChars)) return null

      val host: String
      val port: String?
      if (authority.startsWith('[')) {
        val end = authority.indexOf(']')
        if (end < 0) return null
        host = authority.substring(0, end + 1)
        val afterHost = authority.substring(end + 1)
        port = when {
          afterHost.isEmpty() -> null
          afterHost.startsWith(':') -> afterHost.substring(1)
          else -> return null
        }
      } else {
        host = authority.substringBefore(':')
        port = if (':' in authority) authority.substringAfter(':') else null
      }

      val validHost = if (host.startsWith('[')) {
        host.matches(ipv6Literal)
      } else {
        host.isNotEmpty() && host.split('.').all { it.length <= MAX_LABEL_LENGTH && it.matches(hostLabel) }
      }
      if (!validHost) return null
      if (port != null && (port.isEmpty() || !port.all(Char::isDigit) || port.toIntOrNull() !in 1..MAX_PORT)) {
        return null
      }

      return HTTPS + authority + path
    }

    /** Service-token values travel as header values: printable ASCII with no spaces. */
    private fun isHeaderToken(value: String): Boolean =
      value.isNotEmpty() && value.all { it in '!'..'~' }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
  }
}

/** What a setup link turned out to hold. */
sealed interface ConnectLinkResult {

  data class Valid(val link: ConnectLink) : ConnectLinkResult

  /** A link in a format from a newer Campfire. */
  data object UnsupportedVersion : ConnectLinkResult

  data class Invalid(val reason: Reason) : ConnectLinkResult

  enum class Reason {
    /** The fragment couldn't be decoded, repeats a field, or has no version. */
    Malformed,

    /** The server isn't an https URL with a valid host. */
    InvalidServer,

    /** The Client ID or Client Secret is missing or isn't a valid header value. */
    InvalidServiceToken,
  }
}
