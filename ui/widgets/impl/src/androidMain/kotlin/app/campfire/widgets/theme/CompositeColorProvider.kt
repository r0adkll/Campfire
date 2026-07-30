// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.glance.unit.ColorProvider

class CompositeColorProvider(
  private val delegate: ColorProvider,
  private val background: Color,
) : ColorProvider {

  override fun getColor(context: Context): Color {
    return delegate.getColor(context)
      .compositeOver(background)
  }
}

fun ColorProvider.compositeOver(background: Color): ColorProvider {
  return CompositeColorProvider(this, background)
}
