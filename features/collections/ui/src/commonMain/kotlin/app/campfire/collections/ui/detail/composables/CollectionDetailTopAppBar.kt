// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.collections.ui.detail.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.IconButtonTooltip
import campfire.features.collections.ui.generated.resources.Res
import campfire.features.collections.ui.generated.resources.action_back
import campfire.features.collections.ui.generated.resources.action_delete_collection
import org.jetbrains.compose.resources.stringResource

@Composable
fun CollectionDetailTopAppBar(
  name: String,
  canEdit: Boolean,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CampfireTopAppBar(
    modifier = modifier,
    title = { Text(name) },
    scrollBehavior = scrollBehavior,
    navigationIcon = {
      val backLabel = stringResource(Res.string.action_back)
      IconButtonTooltip(text = backLabel) {
        IconButton(
          onClick = onBack,
        ) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backLabel)
        }
      }
    },
    actions = {
      if (canEdit) {
        val deleteLabel = stringResource(Res.string.action_delete_collection)
        IconButtonTooltip(text = deleteLabel) {
          IconButton(
            onClick = onDelete,
          ) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = deleteLabel,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
  )
}
