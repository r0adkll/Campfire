// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.find.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CloudDownload
import campfire.features.podcasts.ui.generated.resources.Res
import campfire.features.podcasts.ui.generated.resources.find_episodes_add_to_server
import campfire.features.podcasts.ui.generated.resources.find_episodes_clear_selection
import campfire.features.podcasts.ui.generated.resources.find_episodes_selected_episodes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SelectionBottomBar(
  selectedCount: Int,
  isQueuingDownload: Boolean,
  onClearClick: () -> Unit,
  onDownloadClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = selectedCount > 0,
    enter = slideInVertically { it },
    exit = slideOutVertically { it },
    modifier = modifier,
  ) {
    Surface(
      tonalElevation = 3.dp,
      shadowElevation = 3.dp,
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .navigationBarsPadding(),
      ) {
        Text(
          text = stringResource(Res.string.find_episodes_selected_episodes, selectedCount),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )

        Spacer(Modifier.weight(1f))

        TextButton(
          onClick = onClearClick,
          enabled = !isQueuingDownload,
        ) {
          Text(stringResource(Res.string.find_episodes_clear_selection))
        }

        val downloadButtonSize = ButtonDefaults.MinHeight
        Button(
          onClick = onDownloadClick,
          enabled = !isQueuingDownload,
          shapes = ButtonDefaults.shapes(),
          contentPadding = ButtonDefaults.contentPaddingFor(downloadButtonSize, hasStartIcon = true),
          modifier = Modifier.heightIn(downloadButtonSize),
        ) {
          if (isQueuingDownload) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier.size(ButtonDefaults.iconSizeFor(downloadButtonSize)),
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Icon(
              CampfireIcons.Rounded.CloudDownload,
              contentDescription = null,
              modifier = Modifier.size(ButtonDefaults.iconSizeFor(downloadButtonSize)),
            )
          }
          Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(downloadButtonSize)))
          Text(
            text = stringResource(Res.string.find_episodes_add_to_server),
            style = ButtonDefaults.textStyleFor(downloadButtonSize),
          )
        }
      }
    }
  }
}
