package app.campfire.core.permission

/**
 * Best-effort, multiplatform check for whether [serverUrl] points at a private / local-network
 * host — the addresses gated by Android's Local Network Protection. Covers RFC 1918 IPv4 ranges,
 * loopback, link-local, IPv6 loopback/ULA/link-local, and common local hostnames.
 *
 * Intentionally conservative: it parses the host out of a URL-ish string without a full URL
 * library so it can live in `:core`. Unknown/public hosts return `false`.
 */
fun isPrivateNetworkAddress(serverUrl: String): Boolean {
  val host = extractHost(serverUrl)?.lowercase() ?: return false

  if (host == "localhost" ||
    host.endsWith(".local") ||
    host.endsWith(".lan") ||
    host.endsWith(".home") ||
    host.endsWith(".internal")
  ) {
    return true
  }

  // IPv6 (already unbracketed by extractHost)
  if (host.contains(':')) {
    if (host == "::1") return true
    // Unique-local fc00::/7 (fc/fd) and link-local fe80::/10.
    return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")
  }

  // IPv4
  val octets = host.split('.')
  if (octets.size == 4 && octets.all { it.toIntOrNull() in 0..255 }) {
    val a = octets[0].toInt()
    val b = octets[1].toInt()
    return when {
      a == 10 -> true // 10.0.0.0/8
      a == 127 -> true // loopback 127.0.0.0/8
      a == 192 && b == 168 -> true // 192.168.0.0/16
      a == 172 && b in 16..31 -> true // 172.16.0.0/12
      a == 169 && b == 254 -> true // link-local 169.254.0.0/16
      else -> false
    }
  }

  return false
}

/** Pulls the bare host out of a possibly-schemed `host:port/path` string. */
private fun extractHost(serverUrl: String): String? {
  var s = serverUrl.trim()
  if (s.isEmpty()) return null

  // Strip scheme
  val schemeIdx = s.indexOf("://")
  if (schemeIdx >= 0) s = s.substring(schemeIdx + 3)

  // Strip path / query / fragment
  s = s.substringBefore('/').substringBefore('?').substringBefore('#')

  // Strip userinfo
  s = s.substringAfterLast('@')
  if (s.isEmpty()) return null

  // Bracketed IPv6: [::1]:8080 -> ::1
  if (s.startsWith('[')) {
    val end = s.indexOf(']')
    if (end > 0) return s.substring(1, end)
    return null
  }

  // host:port (IPv4/hostname) -> host. A bare unbracketed IPv6 has multiple ':' — keep as-is.
  return if (s.count { it == ':' } == 1) s.substringBefore(':') else s
}
