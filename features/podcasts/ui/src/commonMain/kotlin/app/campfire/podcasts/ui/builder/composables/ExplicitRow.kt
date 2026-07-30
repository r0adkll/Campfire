// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.builder.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_explicit_description
import campfire.features.podcasts.ui.generated.resources.add_podcast_builder_explicit_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExplicitRow(
  enabled: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  shape: Shape = MaterialTheme.shapes.medium,
) {
  Card(
    shape = shape,
    modifier = modifier.fillMaxWidth(),
    onClick = { onToggle(!enabled) },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(
        horizontal = 16.dp,
        vertical = 16.dp,
      ),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(Res.string.add_podcast_builder_explicit_label),
          style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = stringResource(Res.string.add_podcast_builder_explicit_description),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(16.dp))
      Switch(checked = enabled, onCheckedChange = onToggle)
    }
  }
}
