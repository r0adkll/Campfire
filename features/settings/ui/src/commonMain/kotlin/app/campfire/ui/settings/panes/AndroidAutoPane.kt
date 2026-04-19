package app.campfire.ui.settings.panes

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.settings.api.AndroidAutoCategory
import app.campfire.settings.api.AndroidAutoCategoryConfig
import app.campfire.ui.settings.SettingsUiEvent.AndroidAutoSettingEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.Header
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.android_auto_categories_description
import campfire.features.settings.ui.generated.resources.android_auto_category_authors
import campfire.features.settings.ui.generated.resources.android_auto_category_collections
import campfire.features.settings.ui.generated.resources.android_auto_category_downloads
import campfire.features.settings.ui.generated.resources.android_auto_category_home
import campfire.features.settings.ui.generated.resources.android_auto_category_playlists
import campfire.features.settings.ui.generated.resources.android_auto_category_series
import campfire.features.settings.ui.generated.resources.android_auto_drag_handle_description
import campfire.features.settings.ui.generated.resources.android_auto_header_categories
import campfire.features.settings.ui.generated.resources.android_auto_layout_grid
import campfire.features.settings.ui.generated.resources.android_auto_layout_list
import campfire.features.settings.ui.generated.resources.android_auto_open_settings
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_step_1
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_step_2
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_step_3
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_step_4
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_step_5
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_warning_body
import campfire.features.settings.ui.generated.resources.android_auto_unavailable_warning_title
import campfire.features.settings.ui.generated.resources.setting_android_auto_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn

@Composable
internal fun AndroidAutoPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_android_auto_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    if (!state.androidAutoSettings.isAndroidAutoAvailable) {
      UnavailableWarningCard()
    }

    ActionSetting(
      headlineContent = { Text(stringResource(Res.string.android_auto_open_settings)) },
      leadingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
      onClick = { state.eventSink(AndroidAutoSettingEvent.OpenAndroidAutoSettings) },
    )

    Header(
      title = { Text(stringResource(Res.string.android_auto_header_categories)) },
    )

    Text(
      text = stringResource(Res.string.android_auto_categories_description),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(Modifier.height(8.dp))

    val haptics = LocalHapticFeedback.current
    val categories = state.androidAutoSettings.categories
    ReorderableColumn(
      list = categories,
      onSettle = { from, to ->
        val reordered = categories.toMutableList().apply {
          add(to, removeAt(from))
        }
        state.eventSink(
          AndroidAutoSettingEvent.ReorderCategories(reordered.map { it.category }),
        )
      },
      onMove = {
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
      },
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 16.dp),
    ) { index, config, isDragging ->
      val interactionSource = remember { MutableInteractionSource() }

      val content: @Composable (Modifier) -> Unit = { dragHandleModifier ->
        CategoryRow(
          config = config,
          shape = rowShape(index = index, total = categories.size),
          dragHandleModifier = dragHandleModifier,
          onVisibilityChange = { visible ->
            state.eventSink(AndroidAutoSettingEvent.SetCategoryVisible(config.category, visible))
          },
          onGridLayoutChange = { isGrid ->
            state.eventSink(AndroidAutoSettingEvent.SetCategoryGridLayout(config.category, isGrid))
          },
          interactionSource = interactionSource,
        )
      }

      if (config.category.pinned) {
        content(Modifier)
      } else {
        ReorderableItem {
          content(
            Modifier.draggableHandle(
              interactionSource = interactionSource,
              onDragStarted = {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
              },
              onDragStopped = {
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
              },
            ),
          )
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun CategoryRow(
  config: AndroidAutoCategoryConfig,
  shape: Shape,
  dragHandleModifier: Modifier,
  onVisibilityChange: (Boolean) -> Unit,
  onGridLayoutChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  ElevatedCard(
    enabled = config.visible,
    onClick = { },
    shape = shape,
    colors = CardDefaults.elevatedCardColors(
      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.38f),
    ),
    modifier = modifier,
    interactionSource = interactionSource,
  ) {
    ListItem(
      headlineContent = { Text(stringResource(config.category.label)) },
      supportingContent = {
        LayoutChips(
          enabled = config.visible,
          isGridLayout = config.isGridLayout,
          onGridLayoutChange = onGridLayoutChange,
        )
      },
      leadingContent = {
        val description = stringResource(Res.string.android_auto_drag_handle_description)
        Icon(
          imageVector = if (config.category.pinned) {
            Icons.Rounded.PushPin
          } else {
            Icons.Rounded.DragIndicator
          },
          contentDescription = description,
          modifier = dragHandleModifier.semantics { contentDescription = description },
        )
      },
      trailingContent = {
        Switch(
          enabled = !config.category.alwaysVisible,
          checked = config.visible,
          onCheckedChange = onVisibilityChange,
        )
      },
      colors = ListItemDefaults.colors(
        headlineColor = LocalContentColor.current,
        containerColor = Color.Transparent,
      ),
    )
  }
}

@Composable
private fun LayoutChips(
  enabled: Boolean,
  isGridLayout: Boolean,
  onGridLayoutChange: (Boolean) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(top = 4.dp),
  ) {
    FilterChip(
      selected = !isGridLayout,
      onClick = { onGridLayoutChange(false) },
      enabled = enabled,
      label = { Text(stringResource(Res.string.android_auto_layout_list)) },
    )
    FilterChip(
      selected = isGridLayout,
      onClick = { onGridLayoutChange(true) },
      enabled = enabled,
      label = { Text(stringResource(Res.string.android_auto_layout_grid)) },
    )
  }
}

@Composable
private fun UnavailableWarningCard(
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp)
        .height(48.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        Icons.Rounded.Warning,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = stringResource(Res.string.android_auto_unavailable_warning_title),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
    }

    Text(
      text = buildAnnotatedString {
        appendLine(stringResource(Res.string.android_auto_unavailable_warning_body))
        appendBulletedList(
          stringResource(Res.string.android_auto_unavailable_step_1),
          stringResource(Res.string.android_auto_unavailable_step_2),
          stringResource(Res.string.android_auto_unavailable_step_3),
          stringResource(Res.string.android_auto_unavailable_step_4),
          stringResource(Res.string.android_auto_unavailable_step_5),
        )
      },
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    )
  }
}

private fun AnnotatedString.Builder.appendBulletedList(vararg items: String) {
  val bulletParagraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 16.sp))
  items.forEach {
    withStyle(bulletParagraphStyle) {
      append("\u2022")
      append("\t\t")
      append(it)
    }
  }
}

private val AndroidAutoCategory.label: StringResource
  get() = when (this) {
    AndroidAutoCategory.Home -> Res.string.android_auto_category_home
    AndroidAutoCategory.Series -> Res.string.android_auto_category_series
    AndroidAutoCategory.Authors -> Res.string.android_auto_category_authors
    AndroidAutoCategory.Playlists -> Res.string.android_auto_category_playlists
    AndroidAutoCategory.Collections -> Res.string.android_auto_category_collections
    AndroidAutoCategory.Downloads -> Res.string.android_auto_category_downloads
  }

private fun rowShape(index: Int, total: Int): Shape {
  val large = 16.dp
  val small = 4.dp
  return when {
    total == 1 -> RoundedCornerShape(large)
    index == 0 -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
    index == total - 1 ->
      RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
    else -> RoundedCornerShape(small)
  }
}
