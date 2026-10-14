// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android

import android.content.Intent
import android.os.Bundle
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.navigation.ConnectLink
import app.campfire.core.navigation.ConnectLinkResult
import app.campfire.core.navigation.DeepLink
import app.campfire.core.navigation.DeepLinkKeys

/**
 * Translate an [Intent] into a [DeepLink].
 *
 * A setup link (the App Link `https://thescavengers.software/campfire/connect#…`, or its
 * `campfireaudiobooks://connect?…` fallback) is always honored: it only opens a confirmation, and
 * nothing in it is saved until the user agrees. It is ignored when
 * [isRedelivery] (the activity being recreated, or relaunched from Recents with its original
 * intent), so an old link doesn't ask again.
 *
 * `library_item_id` and `campfire_upcoming` are always honored (widgets / notifications use
 * them). The automation actions
 * (`campfire_action=setup|navigate|play|expand_player`) are only honored when [allowAutomation]
 * is true, which callers must tie to `BuildConfig.DEBUG` — they can sign the app into an
 * arbitrary server and must never be reachable from a release build.
 */
internal fun Intent.toDeepLink(
  allowAutomation: Boolean,
  nonce: Long,
  isRedelivery: Boolean = false,
): DeepLink {
  val connectLink = connectLink()
  if (connectLink != null) {
    val launchedFromHistory = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
    if (isRedelivery || launchedFromHistory) return DeepLink.None
    return DeepLink.Connect(connectLink, nonce)
  }

  val extras = extras ?: return DeepLink.None

  val action = extras.getString(DeepLinkKeys.Action)
  if (action == null || !allowAutomation) {
    extras.getString(DeepLinkKeys.LibraryItemId)?.let { return DeepLink.ItemDetail(it) }
    if (extras.getBoolean(DeepLinkKeys.Upcoming, false)) return DeepLink.Upcoming(nonce)
    return DeepLink.None
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

/**
 * The setup link this intent carries, or `null` when it carries none. The URI is read still
 * encoded, so that escaped `&` and `=` inside values survive until the form is decoded, and it
 * goes through the same matching as a scanned QR code.
 */
private fun Intent.connectLink(): ConnectLinkResult? {
  if (action != Intent.ACTION_VIEW) return null
  return ConnectLink.parseUrl(dataString ?: return null)
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
