// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Column
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.campfire.widgets.theme.LocalContentColorProvider

@Composable
internal fun PlaybackInfo(
  title: String,
  subtitle: String,
  supportingText: (@Composable () -> Unit)? = null,
  modifier: GlanceModifier = GlanceModifier,
) {
  Column(
    modifier = modifier,
  ) {
    Text(
      text = title,
      style = TextStyle(
        color = LocalContentColorProvider.current,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
      ),
      maxLines = 2,
    )
    Text(
      text = subtitle,
      style = TextStyle(
        color = LocalContentColorProvider.current,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      ),
      maxLines = 1,
    )

    supportingText?.invoke()
  }
}
