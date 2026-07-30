// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.login

/**
 * Build the list of URLs to probe for a raw server address input, in order.
 *
 * Input that already carries a scheme is probed as-is. Scheme-less input gets both
 * schemes as candidates: LAN-looking hosts (private IP ranges, `.local`, `localhost`)
 * try `http://` first since local Audiobookshelf servers rarely have TLS, while
 * everything else tries `https://` first.
 */
internal fun serverUrlProbeCandidates(input: String): List<String> {
  if (input.contains("://")) return listOf(input)
  return if (isLikelyLanHost(input)) {
    listOf("http://$input", "https://$input")
  } else {
    listOf("https://$input", "http://$input")
  }
}

internal fun isLikelyLanHost(input: String): Boolean {
  val host = input
    .substringBefore('/')
    .substringBefore(':')
    .lowercase()

  return host == "localhost" ||
    host.endsWith(".local") ||
    host.startsWith("192.168.") ||
    host.startsWith("10.") ||
    PrivateClassBRegex.matches(host)
}

// 172.16.0.0 – 172.31.255.255
private val PrivateClassBRegex = Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\..*")
