// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

class AlphaColorProvider(
  private val delegate: ColorProvider,
  private val alpha: Float,
) : ColorProvider {

  override fun getColor(context: Context): Color {
    return delegate.getColor(context).copy(alpha = alpha)
  }
}

fun ColorProvider.withAlpha(alpha: Float): ColorProvider {
  return AlphaColorProvider(this, alpha)
}
