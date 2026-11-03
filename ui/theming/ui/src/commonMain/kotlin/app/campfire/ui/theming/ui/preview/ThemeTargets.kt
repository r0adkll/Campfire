// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.ui.builder.color.ContrastCheck

/**
 * How the parts of a [ThemePreviewContent] respond: which ones get a warning outline because
 * their contrast check fails, and what happens when one is tapped.
 */
@Immutable
internal data class ThemeTargets(
  val failing: Set<ContrastCheck> = emptySet(),
  val onClick: ((ContrastCheck) -> Unit)? = null,
) {
  companion object {
    val None = ThemeTargets()
  }
}

/**
 * Marks this element as the part of the preview that [check] covers. Place it before the
 * element's background so the warning outline draws on top.
 */
internal fun Modifier.themeTarget(
  check: ContrastCheck,
  targets: ThemeTargets,
  cornerRadius: Dp = 0.dp,
): Modifier {
  val outlined = if (check in targets.failing) {
    drawWithContent {
      drawContent()
      drawRoundRect(
        color = WarningOutlineColor,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
          width = 2.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
      )
    }
  } else {
    this
  }

  val onClick = targets.onClick ?: return outlined
  return outlined.clickable(role = Role.Button) { onClick(check) }
}

/** Amber reads against both light and dark themes, whatever their accent. */
private val WarningOutlineColor = Color(0xFFF59E0B)
