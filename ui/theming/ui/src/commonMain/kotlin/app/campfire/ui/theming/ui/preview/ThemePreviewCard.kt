// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DarkMode
import app.campfire.common.compose.icons.rounded.LightMode
import app.campfire.ui.theming.api.AppTheme
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_preview_dark_mode
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_book
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_everywhere
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_home
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_library
import campfire.ui.theming.ui.generated.resources.theme_preview_scene_player
import campfire.ui.theming.ui.generated.resources.theme_preview_title
import org.jetbrains.compose.resources.stringResource

/** Tall enough to show most of a scene; each scene scrolls for the rest. */
internal val ThemePreviewHeight = 420.dp

/**
 * The theme preview with its scene tabs and a light/dark switch, for screens that show a finished
 * theme rather than edit one. Wrap it in the theme being previewed.
 */
@Composable
fun ThemePreviewCard(
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
  content: PreviewContent,
  icon: AppTheme.Icon,
  modifier: Modifier = Modifier,
  onCoverThemingClick: (() -> Unit)? = null,
) {
  var scene by rememberSaveable { mutableStateOf(PreviewScene.Home) }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(Res.string.theme_preview_title),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier
            .weight(1f)
            .padding(start = 4.dp),
        )
        DarkModeSwitch(isDarkMode = isDarkMode, onDarkModeChange = onDarkModeChange)
      }
      PreviewSceneTabs(scene = scene, onSceneChange = { scene = it })
      ThemePreview(
        scene = scene,
        content = content,
        icon = icon,
        onCoverThemingClick = onCoverThemingClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(ThemePreviewHeight),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PreviewSceneTabs(
  scene: PreviewScene,
  onSceneChange: (PreviewScene) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    PreviewScene.entries.forEachIndexed { index, option ->
      val label = option.label()
      OutlinedToggleButton(
        checked = option == scene,
        onCheckedChange = { onSceneChange(option) },
        shapes = when (index) {
          0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
          PreviewScene.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
          else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
        },
        colors = ToggleButtonDefaults.outlinedToggleButtonColors(
          checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
          checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 4.dp),
        // Wider labels get wider tabs, so five fit across a phone without cutting any short
        modifier = Modifier.weight(label.length + TabWeightPadding),
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/** Evens out the tabs' widths, so short labels still get room around them. */
private const val TabWeightPadding = 3f

@Composable
internal fun DarkModeSwitch(
  isDarkMode: Boolean,
  onDarkModeChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val label = stringResource(Res.string.theme_preview_dark_mode)
  Switch(
    checked = isDarkMode,
    onCheckedChange = onDarkModeChange,
    thumbContent = {
      Icon(
        if (isDarkMode) CampfireIcons.Rounded.DarkMode else CampfireIcons.Rounded.LightMode,
        contentDescription = null,
        modifier = Modifier.size(SwitchDefaults.IconSize),
      )
    },
    modifier = modifier.semantics { contentDescription = label },
  )
}

@Composable
internal fun PreviewScene.label(): String = stringResource(
  when (this) {
    PreviewScene.Home -> Res.string.theme_preview_scene_home
    PreviewScene.Library -> Res.string.theme_preview_scene_library
    PreviewScene.Everywhere -> Res.string.theme_preview_scene_everywhere
    PreviewScene.Book -> Res.string.theme_preview_scene_book
    PreviewScene.Player -> Res.string.theme_preview_scene_player
  },
)
