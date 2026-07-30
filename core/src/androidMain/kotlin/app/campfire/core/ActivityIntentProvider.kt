// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core

import android.content.Intent

interface ActivityIntentProvider {

  fun provide(): Intent
}
