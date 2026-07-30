// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread

interface ComponentActivityPlugin {
  @MainThread
  fun register(activity: ComponentActivity)

  @MainThread
  fun unregister()
}
