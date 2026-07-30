// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlinx.datetime.LocalDate

val LocalDate.readableFormat: String
  get() = "${month.name.capitalized()} $day, $year"
