// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Branding for book info providers, keyed by the provider's stable string key
 * (`ProviderId.key` in `:data:bookinfo:api`) so this module doesn't depend on
 * the bookinfo api. Multi-color vectors — render with `Image` (or `Icon` with
 * `tint = Color.Unspecified`) so the brand colors survive.
 */
private const val HARDCOVER = "hardcover"

fun providerBrandIcon(providerKey: String): ImageVector? = when (providerKey) {
  HARDCOVER -> CampfireIcons.Hardcover
  else -> null
}

/** The provider's primary brand color (Hardcover declares #6366F1 as its theme color). */
fun providerBrandColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color(0xFF6366F1)
  else -> null
}

/** The provider's primary brand color (Hardcover declares #6366F1 as its theme color). */
fun providerBrandSecondaryColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color(0xFF312E81)
  else -> null
}

/** Content color that stays legible on top of [providerBrandColor]. */
fun providerOnBrandColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color.White
  else -> null
}
