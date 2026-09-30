// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.auth.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TitleDivider(
  title: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .padding(vertical = 16.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      Modifier.weight(1f),
    )

    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 16.dp),
      color = MaterialTheme.colorScheme.outlineVariant,
    )

    HorizontalDivider(
      Modifier.weight(1f),
    )
  }
}
