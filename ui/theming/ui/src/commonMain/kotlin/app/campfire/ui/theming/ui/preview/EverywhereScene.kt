// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.filled.Home
import app.campfire.common.compose.icons.rounded.CloudOff
import app.campfire.common.compose.icons.rounded.QueryStats
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_back_online
import campfire.ui.theming.ui.generated.resources.theme_preview_cancel
import campfire.ui.theming.ui.generated.resources.theme_preview_cover_failed
import campfire.ui.theming.ui.generated.resources.theme_preview_create
import campfire.ui.theming.ui.generated.resources.theme_preview_downloads_header
import campfire.ui.theming.ui.generated.resources.theme_preview_drawer_home
import campfire.ui.theming.ui.generated.resources.theme_preview_drawer_statistics
import campfire.ui.theming.ui.generated.resources.theme_preview_new_playlist
import campfire.ui.theming.ui.generated.resources.theme_preview_playlist_exists
import campfire.ui.theming.ui.generated.resources.theme_preview_playlist_name
import campfire.ui.theming.ui.generated.resources.theme_preview_playlist_road_trip
import campfire.ui.theming.ui.generated.resources.theme_preview_server_name
import campfire.ui.theming.ui.generated.resources.theme_preview_stream_offline
import campfire.ui.theming.ui.generated.resources.theme_preview_user_name
import campfire.ui.theming.ui.generated.resources.theme_preview_wifi_only
import campfire.ui.theming.ui.generated.resources.theme_preview_working_offline
import org.jetbrains.compose.resources.stringResource

/**
 * The parts every screen shares, in the colors the app gives them, side by side: the navigation
 * drawer with its account card and offline status over a settings section with switches, beside
 * a dialog with a field error, a cover that won't load and a snackbar.
 */
@Composable
internal fun EverywhereScene(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      PreviewDrawer(targets)
      PreviewSettingsSection(targets)
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      PreviewDialog(targets)
      PreviewBrokenCover(targets)
      Snackbar(
        modifier = Modifier.themeTarget(ContrastCheck.Snackbars, targets, cornerRadius = 4.dp),
      ) {
        Text(stringResource(Res.string.theme_preview_back_online))
      }
    }
  }
}

/** The top of the navigation drawer: the account card, a few destinations and offline status. */
@Composable
private fun PreviewDrawer(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    color = colors.surfaceContainerLow,
    shape = MaterialTheme.shapes.large,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(vertical = 12.dp)) {
      Surface(
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .themeTarget(ContrastCheck.AccountCard, targets, cornerRadius = 16.dp),
      ) {
        Column(Modifier.padding(16.dp)) {
          Text(
            text = stringResource(Res.string.theme_preview_server_name),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = PaytoneOneFontFamily,
          )
          Text(
            text = stringResource(Res.string.theme_preview_user_name),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      Spacer(Modifier.size(8.dp))
      NavigationDrawerItem(
        icon = { Icon(CampfireIcons.Filled.Home, contentDescription = null) },
        label = { Text(stringResource(Res.string.theme_preview_drawer_home)) },
        selected = true,
        onClick = {},
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .themeTarget(ContrastCheck.SelectedItems, targets, cornerRadius = 28.dp),
      )
      NavigationDrawerItem(
        icon = { Icon(CampfireIcons.Rounded.QueryStats, contentDescription = null) },
        label = { Text(stringResource(Res.string.theme_preview_drawer_statistics)) },
        selected = false,
        onClick = {},
        modifier = Modifier
          .padding(horizontal = 12.dp)
          .themeTarget(ContrastCheck.DrawerItems, targets, cornerRadius = 28.dp),
      )

      Spacer(Modifier.size(8.dp))
      Surface(
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp)
          .themeTarget(ContrastCheck.OfflineStatus, targets, cornerRadius = 16.dp),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(CampfireIcons.Rounded.CloudOff, contentDescription = null)
          Spacer(Modifier.width(16.dp))
          Text(
            text = stringResource(Res.string.theme_preview_working_offline),
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }
    }
  }
}

/** A settings section as `SwitchSetting` draws it: a primary header over switch rows. */
@Composable
private fun PreviewSettingsSection(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Column(modifier) {
    Text(
      text = stringResource(Res.string.theme_preview_downloads_header),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .padding(16.dp)
        .themeTarget(ContrastCheck.Headers, targets),
    )
    PreviewSwitchRow(
      title = stringResource(Res.string.theme_preview_wifi_only),
      checked = true,
      targets = targets,
    )
    PreviewSwitchRow(
      title = stringResource(Res.string.theme_preview_stream_offline),
      checked = false,
      targets = targets,
    )
  }
}

@Composable
private fun PreviewSwitchRow(
  title: String,
  checked: Boolean,
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  ListItem(
    headlineContent = { Text(title, maxLines = 2) },
    trailingContent = {
      Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = Modifier.themeTarget(
          if (checked) ContrastCheck.Buttons else ContrastCheck.Borders,
          targets,
          cornerRadius = 16.dp,
        ),
      )
    },
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    modifier = modifier.themeTarget(ContrastCheck.ScreenText, targets),
  )
}

/** A dialog as M3's `AlertDialog` draws it, here with a name that's already taken. */
@Composable
private fun PreviewDialog(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    color = colors.surfaceContainerHigh,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(20.dp)) {
      Text(
        text = stringResource(Res.string.theme_preview_new_playlist),
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(Modifier.size(16.dp))
      OutlinedTextField(
        value = stringResource(Res.string.theme_preview_playlist_road_trip),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        isError = true,
        label = { Text(stringResource(Res.string.theme_preview_playlist_name)) },
        supportingText = {
          Text(
            text = stringResource(Res.string.theme_preview_playlist_exists),
            modifier = Modifier.themeTarget(ContrastCheck.ErrorText, targets),
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .focusProperties { canFocus = false },
      )
      Row(
        modifier = Modifier
          .align(Alignment.End)
          .padding(top = 8.dp)
          .themeTarget(ContrastCheck.DialogButtons, targets),
      ) {
        TextButton(onClick = {}) { Text(stringResource(Res.string.theme_preview_cancel)) }
        TextButton(onClick = {}) { Text(stringResource(Res.string.theme_preview_create)) }
      }
    }
  }
}

/** A book whose cover won't load, as `ImageError` marks it. */
@Composable
private fun PreviewBrokenCover(
  targets: ThemeTargets,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .themeTarget(ContrastCheck.Warnings, targets, cornerRadius = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(CampfireIcons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(32.dp))
        }
      }
    }
    Spacer(Modifier.width(16.dp))
    Text(
      text = stringResource(Res.string.theme_preview_cover_failed),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}
