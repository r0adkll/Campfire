// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.extensions.seconds
import kotlin.time.Duration

data class Chapter(
  val id: Int,
  val start: Float,
  val end: Float,
  val title: String,
) {

  val duration: Duration = (end - start).seconds
}
