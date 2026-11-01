// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.ui.theming.ui.builder.ContrastStatus
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_readability_chip_fail
import campfire.ui.theming.ui.generated.resources.theme_readability_chip_pass
import campfire.ui.theming.ui.generated.resources.theme_readability_fail
import campfire.ui.theming.ui.generated.resources.theme_readability_fail_detail
import campfire.ui.theming.ui.generated.resources.theme_readability_pass
import campfire.ui.theming.ui.generated.resources.theme_readability_pass_detail
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Whether every part of the theme is readable in both modes, or how many parts aren't. It sits
 * over the preview so the verdict stays in sight whatever control is open. Tapping it calls
 * [onClick], to fine-tune what's hard to read.
 *
 * @param compact drops the words, leaving the icon and how many parts are hard to read, for
 * narrow screens where the scene tabs need the room.
 */
@Composable
internal fun ReadabilityChip(
  contrast: List<ContrastStatus>,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  val failing = contrast.count { !it.passesBoth }
  val colors = MaterialTheme.colorScheme
  val containerColor = if (failing == 0) colors.surfaceContainerHigh else colors.errorContainer
  val contentColor = if (failing == 0) colors.onSurfaceVariant else colors.onErrorContainer
  val description = if (failing == 0) {
    stringResource(Res.string.theme_readability_pass, contrast.size) + ". " +
      stringResource(Res.string.theme_readability_pass_detail)
  } else {
    pluralStringResource(Res.plurals.theme_readability_fail, failing, failing) + ". " +
      stringResource(Res.string.theme_readability_fail_detail)
  }

  val content = @Composable {
    val label = when {
      compact && failing == 0 -> null
      compact -> failing.toString()
      failing == 0 -> stringResource(Res.string.theme_readability_chip_pass)
      else -> stringResource(Res.string.theme_readability_chip_fail, failing)
    }
    Row(
      modifier = Modifier
        .height(32.dp)
        .padding(start = 8.dp, end = if (label == null) 8.dp else 12.dp)
        .clearAndSetSemantics { contentDescription = description },
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        if (failing == 0) CampfireIcons.Rounded.CheckCircle else CampfireIcons.Rounded.Warning,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      if (label != null) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
      }
    }
  }

  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = CircleShape,
      color = containerColor,
      contentColor = contentColor,
      modifier = modifier,
      content = content,
    )
  } else {
    Surface(
      shape = CircleShape,
      color = containerColor,
      contentColor = contentColor,
      modifier = modifier,
      content = content,
    )
  }
}
