// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.ui.theming.ui.builder.ContrastStatus
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_readability_dark
import campfire.ui.theming.ui.generated.resources.theme_readability_fail
import campfire.ui.theming.ui.generated.resources.theme_readability_fail_detail
import campfire.ui.theming.ui.generated.resources.theme_readability_light
import campfire.ui.theming.ui.generated.resources.theme_readability_pass
import campfire.ui.theming.ui.generated.resources.theme_readability_pass_detail
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A one-line verdict for both modes, then each failing part with its ratios. */
@Composable
internal fun ReadabilitySummary(
  contrast: List<ContrastStatus>,
  modifier: Modifier = Modifier,
) {
  val failing = contrast.filterNot { it.passesBoth }
  val colors = MaterialTheme.colorScheme
  Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = if (failing.isEmpty()) colors.surfaceContainerHigh else colors.errorContainer,
      contentColor = if (failing.isEmpty()) colors.onSurface else colors.onErrorContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          if (failing.isEmpty()) CampfireIcons.Rounded.CheckCircle else CampfireIcons.Rounded.Warning,
          contentDescription = null,
          tint = if (failing.isEmpty()) colors.primary else colors.onErrorContainer,
        )
        Column {
          Text(
            text = if (failing.isEmpty()) {
              stringResource(Res.string.theme_readability_pass, contrast.size)
            } else {
              pluralStringResource(Res.plurals.theme_readability_fail, failing.size, failing.size)
            },
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = if (failing.isEmpty()) {
              stringResource(Res.string.theme_readability_pass_detail)
            } else {
              stringResource(Res.string.theme_readability_fail_detail)
            },
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    failing.forEach { status ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = status.check.label(),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = listOfNotNull(
            stringResource(Res.string.theme_readability_light, status.lightRatio.ratioText())
              .takeUnless { status.passes(dark = false) },
            stringResource(Res.string.theme_readability_dark, status.darkRatio.ratioText())
              .takeUnless { status.passes(dark = true) },
          ).joinToString(" · "),
          style = MaterialTheme.typography.labelMedium,
          color = colors.error,
        )
      }
    }
  }
}
