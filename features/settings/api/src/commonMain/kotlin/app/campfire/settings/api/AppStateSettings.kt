// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.StateFlow

/**
 * What the app has already shown or offered the user, so it doesn't do so again.
 */
interface AppStateSettings {

  var hasShownWidgetPinning: Boolean
  fun observeHasShownWidgetPinning(): StateFlow<Boolean>

  var lastSeenVersion: String?
  fun observeLastSeenVersion(): StateFlow<String?>

  /**
   * When `true`, the user has dismissed the app update sign-in prompt and it should
   * no longer be shown.
   */
  var appUpdateSignInDismissed: Boolean
  fun observeAppUpdateSignInDismissed(): StateFlow<Boolean>

  /**
   * The versionCode of the last app update the user dismissed from the update widget.
   * The widget stays hidden for that release but shows again for a different one.
   * `0` when no update has been dismissed.
   */
  var appUpdateDismissedVersionCode: Long
  fun observeAppUpdateDismissedVersionCode(): StateFlow<Long>
}
