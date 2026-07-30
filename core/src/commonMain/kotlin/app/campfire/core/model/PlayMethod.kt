// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

enum class PlayMethod(val serverValue: Int) {
  DirectPlay(0),
  DirectStream(1),
  Transcode(2),
  Local(3),
}
