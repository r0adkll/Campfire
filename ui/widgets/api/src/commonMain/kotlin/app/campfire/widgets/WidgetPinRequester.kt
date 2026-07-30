// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets

interface WidgetPinRequester {

  /**
   * Request the system UI to show the playback control widget to the user
   * to be pinned on the homescreen. This is currently only working in Android.
   */
  fun requestPinWidget()
}
