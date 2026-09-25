// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.reachability

import io.ktor.http.Url

/** Where a server lives, judged from its address alone — no DNS lookup. */
internal enum class ServerLocality {
  /** A private/LAN address or local-only hostname: reachable from its own network (or a VPN into it). */
  Private,

  /** A Tailscale (CGNAT) address or MagicDNS name: reachable only through the VPN. */
  VpnOnly,

  /** Anything else, including hostnames that may resolve anywhere; never restricted. */
  Public,
  ;

  companion object {

    fun of(serverUrl: String): ServerLocality {
      val host = runCatching { Url(serverUrl).host }.getOrNull() ?: return Public
      return ofHost(host)
    }

    fun ofHost(rawHost: String): ServerLocality {
      val host = rawHost.trim('[', ']').trimEnd('.').lowercase()
      ipv4(host)?.let { return ofIpv4(it) }
      if (':' in host) return ofIpv6(host)
      return when {
        host.endsWith(".ts.net") -> VpnOnly
        LOCAL_SUFFIXES.any { host.endsWith(it) } -> Private
        else -> Public
      }
    }

    private fun ofIpv4(octets: IntArray): ServerLocality {
      val (a, b) = octets
      return when {
        a == 10 -> Private
        a == 172 && b in 16..31 -> Private
        a == 192 && b == 168 -> Private
        a == 169 && b == 254 -> Private
        a == 100 && b in 64..127 -> VpnOnly
        else -> Public
      }
    }

    private fun ofIpv6(host: String): ServerLocality {
      val firstGroup = host.substringBefore(':').ifEmpty { return Public }.toIntOrNull(16) ?: return Public
      return when {
        // fc00::/7 unique local
        firstGroup and 0xFE00 == 0xFC00 -> Private
        // fe80::/10 link-local
        firstGroup and 0xFFC0 == 0xFE80 -> Private
        else -> Public
      }
    }

    private fun ipv4(host: String): IntArray? {
      val parts = host.split('.')
      if (parts.size != 4) return null
      val octets = IntArray(4)
      parts.forEachIndexed { index, part ->
        val value = part.toIntOrNull() ?: return null
        if (value !in 0..255) return null
        octets[index] = value
      }
      return octets
    }

    private val LOCAL_SUFFIXES = listOf(".local", ".lan", ".home.arpa", ".internal")
  }
}
