// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.AutoAwesome
import app.campfire.common.compose.icons.rounded.Edit
import app.campfire.common.compose.icons.rounded.FormatPaint
import app.campfire.common.compose.icons.theme.Ai
import app.campfire.common.compose.theme.LocalUseDarkColors
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.ui.theming.api.AppTheme
import app.campfire.ui.theming.api.AppThemeImage
import app.campfire.ui.theming.api.colorScheme
import app.campfire.ui.theming.ui.layout.DualPreviewGap
import app.campfire.ui.theming.ui.preview.PreviewScene
import app.campfire.ui.theming.ui.preview.PreviewSceneTabs
import app.campfire.ui.theming.ui.preview.ThemePreview
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.themes_customize
import campfire.ui.theming.ui.generated.resources.themes_edit
import campfire.ui.theming.ui.generated.resources.themes_mode_dark
import campfire.ui.theming.ui.generated.resources.themes_mode_light
import org.jetbrains.compose.resources.stringResource

/**
 * The theme in use, large: its name with Customize for a built-in theme or Edit for one of the
 * user's, the preview's scenes, and the preview itself. With [dual], light and dark show side by
 * side; otherwise the preview follows the app's own light or dark setting.
 */
@Composable
internal fun ThemeDetail(
  state: ThemePickerUiState,
  dual: Boolean,
  modifier: Modifier = Modifier,
) {
  val theme = state.currentTheme
  var scene by rememberSaveable { mutableStateOf(PreviewScene.Home) }
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    BoxWithConstraints {
      // A narrow pane, beside the list on an unfolded foldable, keeps the room for the name.
      val compact = maxWidth < CompactHeaderWidth
      Row(verticalAlignment = Alignment.CenterVertically) {
        AppThemeImage(appTheme = theme, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Text(
          text = theme.displayName(),
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .semantics { heading() },
        )
        Spacer(Modifier.width(8.dp))
        if (theme is AppTheme.Fixed && theme.isUserTheme) {
          DetailAction(
            label = stringResource(Res.string.themes_edit),
            icon = if (theme is AppTheme.Fixed.Ai) CampfireIcons.Rounded.AutoAwesome else CampfireIcons.Rounded.Edit,
            compact = compact,
            onClick = { state.eventSink(ThemePickerUiEvent.OpenThemeBuilder(theme)) },
          )
        } else {
          val seedColor = theme.seedColor()
          DetailAction(
            label = stringResource(Res.string.themes_customize),
            icon = CampfireIcons.Rounded.FormatPaint,
            compact = compact,
            onClick = { state.eventSink(ThemePickerUiEvent.Customize(theme, seedColor)) },
          )
        }
      }
    }

    PreviewSceneTabs(
      scene = scene,
      onSceneChange = { scene = it },
      modifier = Modifier.widthIn(max = ModeButtonsMaxWidth),
    )

    if (dual) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(DualPreviewGap),
      ) {
        listOf(false, true).forEach { dark ->
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = stringResource(if (dark) Res.string.themes_mode_dark else Res.string.themes_mode_light),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailPreview(
              state = state,
              scene = scene,
              dark = dark,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            )
          }
        }
      }
    } else {
      DetailPreview(
        state = state,
        scene = scene,
        dark = LocalUseDarkColors.current,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      )
    }
  }
}

/** The header's action: a labelled button, or with [compact], an icon button with a tooltip. */
@Composable
private fun DetailAction(
  label: String,
  icon: ImageVector,
  compact: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (compact) {
    IconButtonTooltip(text = label, modifier = modifier) {
      FilledTonalIconButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
      }
    }
  } else {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(label)
    }
  }
}

/** Below this, the header's action shows as an icon, so the theme's name has room. */
private val CompactHeaderWidth = 420.dp

@Composable
private fun DetailPreview(
  state: ThemePickerUiState,
  scene: PreviewScene,
  dark: Boolean,
  modifier: Modifier = Modifier,
) {
  val theme = state.currentTheme
  MaterialTheme(colorScheme = colorScheme(theme, dark)) {
    ThemePreview(
      scene = scene,
      content = state.previewContent,
      icon = themeIcon(theme),
      modifier = modifier,
    )
  }
}
