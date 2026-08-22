// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.content.Intent
import android.os.Bundle
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.navigation.DeepLink
import app.campfire.core.navigation.DeepLinkKeys

/**
 * Translate an [Intent]'s extras into a [DeepLink].
 *
 * `library_item_id` is always honored (widgets / notifications use it). The automation actions
 * (`campfire_action=setup|navigate|play|expand_player`) are only honored when [allowAutomation]
 * is true, which callers must tie to `BuildConfig.DEBUG` — they can sign the app into an
 * arbitrary server and must never be reachable from a release build.
 */
internal fun Intent.toDeepLink(allowAutomation: Boolean, nonce: Long): DeepLink {
  val extras = extras ?: return DeepLink.None

  val action = extras.getString(DeepLinkKeys.Action)
  if (action == null || !allowAutomation) {
    return extras.getString(DeepLinkKeys.LibraryItemId)
      ?.let { DeepLink.ItemDetail(it) }
      ?: DeepLink.None
  }

  fun required(key: String): String =
    requireNotNull(extras.getString(key)) { "Deep link action '$action' requires extra '$key'" }

  return try {
    parseAutomation(action, extras, ::required, nonce)
  } catch (e: IllegalArgumentException) {
    bark(LogPriority.WARN, tag = "DeepLink", throwable = e) { "Ignoring malformed automation intent" }
    DeepLink.None
  }
}

private fun parseAutomation(
  action: String,
  extras: Bundle,
  required: (String) -> String,
  nonce: Long,
): DeepLink {
  return when (action) {
    DeepLinkKeys.ActionSetup -> DeepLink.Setup(
      serverUrl = required(DeepLinkKeys.ServerUrl),
      serverName = required(DeepLinkKeys.ServerName),
      username = required(DeepLinkKeys.Username),
      password = required(DeepLinkKeys.Password),
      libraryName = extras.getString(DeepLinkKeys.LibraryName),
      themeMode = extras.getString(DeepLinkKeys.ThemeMode),
      theme = extras.getString(DeepLinkKeys.Theme),
      nonce = nonce,
    )

    DeepLinkKeys.ActionNavigate -> DeepLink.Navigate(
      screen = required(DeepLinkKeys.Screen),
      arg = extras.getString(DeepLinkKeys.ScreenArg),
      nonce = nonce,
    )

    DeepLinkKeys.ActionPlay -> DeepLink.Play(
      libraryItemId = required(DeepLinkKeys.LibraryItemId),
      nonce = nonce,
    )

    DeepLinkKeys.ActionExpandPlayer -> DeepLink.ExpandPlayer(nonce = nonce)

    DeepLinkKeys.ActionStopPlayback -> DeepLink.StopPlayback(nonce = nonce)

    else -> DeepLink.None
  }
}
