// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.navigation

/**
 * The link a Drop build page's Open in App opens, `campfireaudiobooks://drop/<build id>`, to offer
 * that build for install. Drop fills the build's ID into the upload's `open-in-app` template
 * (`campfireaudiobooks://drop/{build}` in .github/workflows), and only the Drop-distributed alpha
 * and beta builds declare the link, in src/preRelease/AndroidManifest.xml.
 */
object OpenBuildLink {

  const val SCHEME = ConnectLink.SCHEME
  const val HOST = "drop"

  private val buildId = Regex("[0-9A-Za-z]+")

  /**
   * The build ID in [url], or `null` when it isn't an Open in App link: the scheme and host,
   * matched apart from case, then the ID as the one path segment, with an optional trailing slash.
   */
  fun parseBuildId(url: String): String? {
    val text = url.trim()
    val schemeEnd = text.indexOf("://")
    if (schemeEnd < 0 || !text.substring(0, schemeEnd).equals(SCHEME, ignoreCase = true)) return null

    val rest = text.substring(schemeEnd + 3)
    val authority = rest.substringBefore('/')
    if (!authority.equals(HOST, ignoreCase = true)) return null

    return rest.substring(authority.length)
      .removePrefix("/")
      .removeSuffix("/")
      .takeIf { buildId.matches(it) }
  }
}
