// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import kotlinx.coroutines.flow.Flow

/**
 * What the app has already shown or offered the user, so it doesn't do so again.
 */
interface AppStateSettings {

  fun setHasShownWidgetPinning(value: Boolean)
  fun observeHasShownWidgetPinning(): Flow<Boolean>

  fun setLastSeenVersion(value: String?)
  fun observeLastSeenVersion(): Flow<String?>

  /**
   * When `true`, the user has dismissed the app update sign-in prompt and it should
   * no longer be shown.
   */
  fun setAppUpdateSignInDismissed(value: Boolean)
  fun observeAppUpdateSignInDismissed(): Flow<Boolean>

  /**
   * The versionCode of the last app update the user dismissed from the update widget.
   * The widget stays hidden for that release but shows again for a different one.
   * `0` when no update has been dismissed.
   */
  fun setAppUpdateDismissedVersionCode(value: Long)
  fun observeAppUpdateDismissedVersionCode(): Flow<Long>
}
