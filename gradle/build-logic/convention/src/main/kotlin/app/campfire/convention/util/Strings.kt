// Copyright 2024, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention.util

import java.util.Locale

fun String.capitalized(): CharSequence = let<CharSequence, CharSequence> {
  if (it.isEmpty()) {
    it
  } else it[0].titlecase(
    Locale.getDefault(),
  ) + it.substring(1)
}
