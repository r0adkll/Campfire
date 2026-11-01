// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.cardElevation

/**
 * @param onLongClick what a long press does, as well as [onClick] for a tap; only taken with an
 * [onClick], the card being the thing that handles its presses
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElevatedContentCard(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onClick: (() -> Unit)? = null,
  shape: Shape = MaterialTheme.shapes.largeIncreased,
  colors: CardColors = CardDefaults.elevatedCardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
  ),
  onLongClick: (() -> Unit)? = null,
  onLongClickLabel: String? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val contentLayout = LocalContentLayout.current
  when {
    onClick != null && onLongClick != null -> ElevatedCard(
      // The card's own click has no long press, so it's handled around it instead
      modifier = modifier
        .clip(shape)
        .combinedClickable(
          enabled = enabled,
          role = Role.Button,
          onLongClickLabel = onLongClickLabel,
          onLongClick = onLongClick,
          onClick = onClick,
        ),
      shape = shape,
      colors = colors,
      elevation = contentLayout.cardElevation,
      content = content,
    )

    onClick != null -> ElevatedCard(
      modifier = modifier,
      enabled = enabled,
      onClick = onClick,
      shape = shape,
      colors = colors,
      elevation = contentLayout.cardElevation,
      content = content,
    )

    else -> ElevatedCard(
      modifier = modifier,
      shape = shape,
      colors = colors,
      elevation = contentLayout.cardElevation,
      content = content,
    )
  }
}
