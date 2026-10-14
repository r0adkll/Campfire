// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

/**
 * Whether two server URLs point at the same server. Server URLs are stored as typed or as the
 * login probe resolved them, so case and a trailing slash don't count.
 */
fun isSameServerUrl(a: String, b: String): Boolean {
  return a.trim().trimEnd('/').equals(b.trim().trimEnd('/'), ignoreCase = true)
}
