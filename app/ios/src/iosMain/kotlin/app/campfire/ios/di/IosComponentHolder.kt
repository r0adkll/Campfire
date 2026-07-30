// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ios.di

import app.campfire.core.di.ComponentHolder

object IosComponentHolder {

  fun addComponent(component: Any) {
    ComponentHolder.components += component
  }
}
