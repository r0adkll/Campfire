// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.navigation

import app.campfire.core.model.LibraryItemId

@Suppress("ConstPropertyName")
object DeepLinkKeys {

  const val LibraryItemId = "library_item_id"

  /**
   * Keys for the debug-only automation deep links. All keys are prefixed `campfire_` because
   * `adb shell am start` parses some bare names (e.g. `username`) as its own options. ([DeepLink.Setup], [DeepLink.Navigate],
   * [DeepLink.Play], [DeepLink.ExpandPlayer]). These are only parsed by debug builds; see
   * tools/screenshots/README.md ("Design decisions").
   */
  const val Action = "campfire_action"
  const val ActionSetup = "setup"
  const val ActionNavigate = "navigate"
  const val ActionPlay = "play"
  const val ActionExpandPlayer = "expand_player"
  const val ActionStopPlayback = "stop_playback"

  const val ServerUrl = "campfire_server_url"
  const val ServerName = "campfire_server_name"
  const val Username = "campfire_username"
  const val Password = "campfire_password"
  const val LibraryName = "campfire_library_name"
  const val ThemeMode = "campfire_theme_mode"
  const val Theme = "campfire_theme"

  const val Screen = "campfire_screen"
  const val ScreenArg = "campfire_screen_arg"
}

sealed interface DeepLink {
  data object None : DeepLink

  data class ItemDetail(
    val libraryItemId: LibraryItemId,
  ) : DeepLink

  /**
   * Debug-only: sign in to [serverUrl] with the given credentials, mark first-run prompts as
   * seen, and optionally select a library by name and apply a theme. The app ends up on Home
   * in a fully configured state.
   *
   * [nonce] makes otherwise-identical requests distinct so each delivered intent is handled.
   */
  data class Setup(
    val serverUrl: String,
    val serverName: String,
    val username: String,
    val password: String,
    val libraryName: String? = null,
    val themeMode: String? = null,
    val theme: String? = null,
    val nonce: Long = 0L,
  ) : DeepLink

  /**
   * Debug-only: push or reset to a named screen. [screen] is a symbolic name (e.g. `home`,
   * `library`, `library_item`, `statistics`, `theme_picker`, `settings`) resolved by the root
   * window; [arg] is the screen's single argument when it takes one (an item id, a settings page).
   */
  data class Navigate(
    val screen: String,
    val arg: String? = null,
    val nonce: Long = 0L,
  ) : DeepLink

  /** Debug-only: start playback of [libraryItemId]. */
  data class Play(
    val libraryItemId: LibraryItemId,
    val nonce: Long = 0L,
  ) : DeepLink

  /** Debug-only: expand the playback bar into the full player. */
  data class ExpandPlayer(
    val nonce: Long = 0L,
  ) : DeepLink

  /** Debug-only: stop the current playback session, if any. */
  data class StopPlayback(
    val nonce: Long = 0L,
  ) : DeepLink
}
