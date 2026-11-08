// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.KeyboardArrowRight
import app.campfire.common.compose.theme.CampfireTheme
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.server_downloads_banner
import org.jetbrains.compose.resources.pluralStringResource

/**
 * How many podcast episodes the server is downloading, shown only while it is, and the way to
 * the server's download queue.
 */
@Composable
fun ServerDownloadsBanner(
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
      CircularProgressIndicator(
        color = LocalContentColor.current,
        trackColor = Color.Transparent,
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.5.dp,
      )
      Text(
        text = pluralStringResource(Res.plurals.server_downloads_banner, count, count),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.weight(1f),
      )
      Icon(
        imageVector = CampfireIcons.Rounded.KeyboardArrowRight,
        contentDescription = null,
      )
    }
  }
}

@Preview
@Composable
private fun ServerDownloadsBannerPreview() = CampfireTheme {
  ServerDownloadsBanner(count = 4, onClick = {})
}
