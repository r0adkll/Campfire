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
private const val AUDIBLE = "audible"

fun providerBrandIcon(providerKey: String): ImageVector? = when (providerKey) {
  HARDCOVER -> CampfireIcons.Hardcover
  AUDIBLE -> CampfireIcons.Audible
  else -> null
}

/**
 * The provider's primary brand color (Hardcover declares #6366F1 as its theme
 * color; Audible's is its signature orange, #F8991C).
 */
fun providerBrandColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color(0xFF6366F1)
  AUDIBLE -> Color(0xFFF8991C)
  else -> null
}

/** A deep shade of the brand hue, mirroring [providerBrandColor]'s scale. */
fun providerBrandSecondaryColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color(0xFF312E81)
  AUDIBLE -> Color(0xFF78350F)
  else -> null
}

/**
 * Content color that stays legible on top of [providerBrandColor]. Audible's
 * orange is light enough that white fails contrast — black is also the
 * pairing Audible itself uses.
 */
fun providerOnBrandColor(providerKey: String): Color? = when (providerKey) {
  HARDCOVER -> Color.White
  AUDIBLE -> Color.Black
  else -> null
}
