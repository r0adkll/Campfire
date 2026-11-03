// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ChevronRight
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.roles
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_all_colors
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_all_colors_detail
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_hint_dark
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_hint_light
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_state_tuned
import campfire.ui.theming.ui.generated.resources.theme_picker_done
import campfire.ui.theming.ui.generated.resources.theme_tune_editing_dark
import campfire.ui.theming.ui.generated.resources.theme_tune_editing_light
import campfire.ui.theming.ui.generated.resources.theme_tune_hard_to_read
import org.jetbrains.compose.resources.stringResource

/**
 * A chip for each part of the app the checks cover, tinted when it's fine-tuned in this mode
 * and dotted red when it's hard to read, then a way into every color.
 *
 * @param overrides the roles fine-tuned in the mode shown.
 * @param failing the checks that fail in the mode shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FineTuneParts(
  overrides: Map<ThemeRole, Color>,
  failing: Set<ContrastCheck>,
  dark: Boolean,
  onPartClick: (ContrastCheck) -> Unit,
  onAllColorsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = stringResource(if (dark) Res.string.theme_fine_tune_hint_dark else Res.string.theme_fine_tune_hint_light),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Each chip's touch target already pads it to 48dp tall, so rows need no gap of their own.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ContrastCheck.entries.forEach { check ->
        PartChip(
          label = check.label(),
          isTuned = check.roles.any { it in overrides },
          isFailing = check in failing,
          onClick = { onPartClick(check) },
        )
      }
    }
    Surface(
      onClick = onAllColorsClick,
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text(stringResource(Res.string.theme_fine_tune_all_colors), style = MaterialTheme.typography.titleSmall)
          Text(
            text = stringResource(Res.string.theme_fine_tune_all_colors_detail),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(CampfireIcons.Rounded.ChevronRight, contentDescription = null)
      }
    }
  }
}

/**
 * [FineTuneParts] for the tool tray: one row of chips that scrolls sideways, the parts that are
 * hard to read first, then All colors.
 */
@Composable
internal fun FineTuneTray(
  overrides: Map<ThemeRole, Color>,
  failing: Set<ContrastCheck>,
  dark: Boolean,
  onPartClick: (ContrastCheck) -> Unit,
  onAllColorsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val parts = ContrastCheck.entries.sortedBy { it !in failing }
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(if (dark) Res.string.theme_fine_tune_hint_dark else Res.string.theme_fine_tune_hint_light),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      parts.forEach { check ->
        PartChip(
          label = check.label(),
          isTuned = check.roles.any { it in overrides },
          isFailing = check in failing,
          onClick = { onPartClick(check) },
        )
      }
      PartChip(
        label = stringResource(Res.string.theme_fine_tune_all_colors),
        isTuned = false,
        isFailing = false,
        onClick = onAllColorsClick,
      )
    }
  }
}

@Composable
private fun PartChip(
  label: String,
  isTuned: Boolean,
  isFailing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val tunedState = stringResource(Res.string.theme_fine_tune_state_tuned)
  val failingState = stringResource(Res.string.theme_tune_hard_to_read)
  val state = listOfNotNull(tunedState.takeIf { isTuned }, failingState.takeIf { isFailing }).joinToString(", ")
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = if (isTuned) colors.secondaryContainer else Color.Transparent,
    contentColor = if (isTuned) colors.onSecondaryContainer else colors.onSurfaceVariant,
    border = if (isTuned) null else BorderStroke(1.dp, colors.outlineVariant),
    modifier = modifier.semantics { if (state.isNotEmpty()) stateDescription = state },
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 32.dp)
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (isFailing) {
        Box(
          Modifier
            .size(8.dp)
            .background(colors.error, CircleShape),
        )
      }
      Text(label, style = MaterialTheme.typography.labelLarge)
    }
  }
}

/**
 * Every role in the theme by its Material name, in the mode being edited, for setting colors the
 * parts don't cover.
 */
@Composable
internal fun AllColorsPanel(
  scheme: ColorScheme,
  overrides: Map<ThemeRole, Color>,
  dark: Boolean,
  onRoleClick: (ThemeRole) -> Unit,
  onDoneClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tunedState = stringResource(Res.string.theme_fine_tune_state_tuned)
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
  ) {
    Row(
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(stringResource(Res.string.theme_fine_tune_all_colors), style = MaterialTheme.typography.titleLarge)
        Text(
          text = stringResource(if (dark) Res.string.theme_tune_editing_dark else Res.string.theme_tune_editing_light),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      FilledTonalButton(onClick = onDoneClick) {
        Text(stringResource(Res.string.theme_picker_done))
      }
    }

    ThemeRole.entries.forEach { role ->
      val isTuned = role in overrides
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(role = Role.Button) { onRoleClick(role) }
          .semantics { if (isTuned) stateDescription = tunedState }
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          Modifier
            .size(32.dp)
            .background(scheme.colorFor(role), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
        )
        Text(
          text = role.tokenName,
          style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier.weight(1f),
        )
        if (isTuned) {
          Text(
            text = tunedState,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = scheme.colorFor(role).hexCode(),
          style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
