// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.util

import platform.Foundation.NSError

fun NSError.asDebugString(): String {
  // AVFoundation/NSURLError failures carry the full stream URL (the user's server) in
  // keys like NSErrorFailingURLKey / NSErrorFailingURLStringKey — drop them from logs.
  val safeUserInfo = userInfo.entries.filterNot { it.key.toString().contains("URL") }
  return """
    NSError(
      code = $code,
      domain = $domain,
      localizedDescription = $localizedDescription,
      localizedFailureReason = ${localizedFailureReason()},
      localizedRecoverySuggestion = $localizedRecoverySuggestion,
      localizedRecoveryOptions = [
        ${localizedRecoveryOptions?.joinToString(separator = "\n") { it.toString() }}
      ],
      userInfo = {
        ${safeUserInfo.joinToString(separator = "\n") { "${it.key} = ${it.value}," }}
      }
    )
  """.trimIndent()
}
