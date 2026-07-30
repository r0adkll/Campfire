// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import app.campfire.core.extensions.fluentIf
import app.campfire.widgets.theme.LocalContentColorProvider

@Composable
internal fun PlayerWidgetScaffold(
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
  backgroundColor: ColorProvider? = GlanceTheme.colors.primaryContainer,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .clickable(onClick)
      .appWidgetBackground()
      .fluentIf(backgroundColor != null) {
        background(backgroundColor!!)
      },
    contentAlignment = Alignment.BottomStart,
  ) {
    val localContentColor = GlanceTheme.colors.onSecondary
    CompositionLocalProvider(
      LocalContentColorProvider provides localContentColor,
    ) {
      content()
    }
  }
}
