// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.ui.theming.api.AppTheme
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_save_confirm
import campfire.ui.theming.ui.generated.resources.theme_save_name
import campfire.ui.theming.ui.generated.resources.theme_save_title
import org.jetbrains.compose.resources.stringResource

/** Names a theme and picks its icon before saving. An unnamed theme gets [suggestedName]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveThemeSheet(
  name: TextFieldState,
  icon: AppTheme.Icon,
  suggestedName: String,
  onIconClick: (AppTheme.Icon) -> Unit,
  onSaveClick: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(Unit) {
    if (name.text.isBlank()) name.setTextAndPlaceCursorAtEnd(suggestedName)
  }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier = modifier,
  ) {
    SaveThemeSheetContent(
      name = name,
      icon = icon,
      onIconClick = onIconClick,
      onSaveClick = onSaveClick,
    )
  }
}

@Composable
internal fun SaveThemeSheetContent(
  name: TextFieldState,
  icon: AppTheme.Icon,
  onIconClick: (AppTheme.Icon) -> Unit,
  onSaveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(stringResource(Res.string.theme_save_title), style = MaterialTheme.typography.headlineSmall)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      IconPicker(icon = icon, onIconClick = onIconClick)
      OutlinedTextField(
        state = name,
        label = { Text(stringResource(Res.string.theme_save_name)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.weight(1f),
      )
    }
    Button(
      onClick = onSaveClick,
      enabled = name.text.isNotBlank(),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(Res.string.theme_save_confirm))
    }
  }
}
