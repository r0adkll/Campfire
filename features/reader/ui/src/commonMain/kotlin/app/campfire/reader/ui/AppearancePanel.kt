// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Add
import app.campfire.common.compose.icons.rounded.Check
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.History
import app.campfire.common.compose.icons.rounded.Remove
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.window.LocalWindowScreenController
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import app.campfire.settings.api.ReaderTheme
import campfire.features.reader.ui.generated.resources.Res
import campfire.features.reader.ui.generated.resources.action_done
import campfire.features.reader.ui.generated.resources.appearance_all_books
import campfire.features.reader.ui.generated.resources.appearance_book_edited
import campfire.features.reader.ui.generated.resources.appearance_book_has_own
import campfire.features.reader.ui.generated.resources.appearance_flow
import campfire.features.reader.ui.generated.resources.appearance_flow_paged
import campfire.features.reader.ui.generated.resources.appearance_flow_scrolled
import campfire.features.reader.ui.generated.resources.appearance_font
import campfire.features.reader.ui.generated.resources.appearance_font_atkinson
import campfire.features.reader.ui.generated.resources.appearance_font_literata
import campfire.features.reader.ui.generated.resources.appearance_font_opendyslexic
import campfire.features.reader.ui.generated.resources.appearance_font_publisher
import campfire.features.reader.ui.generated.resources.appearance_font_sans
import campfire.features.reader.ui.generated.resources.appearance_font_serif
import campfire.features.reader.ui.generated.resources.appearance_hyphenate
import campfire.features.reader.ui.generated.resources.appearance_justify
import campfire.features.reader.ui.generated.resources.appearance_keep_screen_on
import campfire.features.reader.ui.generated.resources.appearance_line_spacing
import campfire.features.reader.ui.generated.resources.appearance_line_spacing_less
import campfire.features.reader.ui.generated.resources.appearance_line_spacing_more
import campfire.features.reader.ui.generated.resources.appearance_margin_narrow
import campfire.features.reader.ui.generated.resources.appearance_margin_normal
import campfire.features.reader.ui.generated.resources.appearance_margin_wide
import campfire.features.reader.ui.generated.resources.appearance_margins
import campfire.features.reader.ui.generated.resources.appearance_pages
import campfire.features.reader.ui.generated.resources.appearance_pages_auto
import campfire.features.reader.ui.generated.resources.appearance_pages_single
import campfire.features.reader.ui.generated.resources.appearance_pages_two
import campfire.features.reader.ui.generated.resources.appearance_reset_book
import campfire.features.reader.ui.generated.resources.appearance_reset_defaults
import campfire.features.reader.ui.generated.resources.appearance_text_larger
import campfire.features.reader.ui.generated.resources.appearance_text_size
import campfire.features.reader.ui.generated.resources.appearance_text_smaller
import campfire.features.reader.ui.generated.resources.appearance_theme
import campfire.features.reader.ui.generated.resources.appearance_theme_black
import campfire.features.reader.ui.generated.resources.appearance_theme_dark
import campfire.features.reader.ui.generated.resources.appearance_theme_follow_app
import campfire.features.reader.ui.generated.resources.appearance_theme_light
import campfire.features.reader.ui.generated.resources.appearance_theme_sepia
import campfire.features.reader.ui.generated.resources.appearance_this_book
import campfire.features.reader.ui.generated.resources.appearance_volume_keys
import campfire.features.reader.ui.generated.resources.appearance_volume_keys_detail
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The reader's appearance settings, over the bottom of the page so their effect shows as they
 * change. Every book or just this one, per [AppearancePanel.scope]; the theme is always every
 * book's.
 */
@Composable
internal fun AppearancePanel(
  panel: AppearancePanel,
  theme: ReaderTheme,
  keepScreenOn: Boolean,
  volumeKeysTurnPages: Boolean,
  eventSink: (ReaderUiEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val layout = panel.layout
  val change = { change: ReaderLayoutChange -> eventSink(ReaderUiEvent.LayoutChanged(change)) }

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier.widthIn(max = 600.dp).fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .heightIn(max = 480.dp)
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
          AppearanceScope.entries.forEachIndexed { index, scope ->
            SegmentedButton(
              selected = panel.scope == scope,
              onClick = { eventSink(ReaderUiEvent.AppearanceScopeChanged(scope)) },
              shape = SegmentedButtonDefaults.itemShape(index, AppearanceScope.entries.size),
            ) {
              Text(stringResource(scope.label))
            }
          }
        }
        Spacer(Modifier.width(12.dp))
        val doneLabel = stringResource(Res.string.action_done)
        IconButtonTooltip(text = doneLabel) {
          FilledTonalIconButton(onClick = { eventSink(ReaderUiEvent.HideAppearance) }) {
            Icon(CampfireIcons.Rounded.Close, contentDescription = doneLabel)
          }
        }
      }

      if (panel.hasOverrides) {
        OverridesNotice(
          scope = panel.scope,
          onReset = { eventSink(ReaderUiEvent.ResetBookLayout) },
        )
      }

      // The theme is always every book's, so it's only offered there
      if (panel.scope == AppearanceScope.AllBooks) {
        Section(Res.string.appearance_theme) {
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReaderTheme.entries.forEach { option ->
              ThemeSwatch(
                theme = option,
                selected = option == theme,
                onClick = { eventSink(ReaderUiEvent.ThemeChanged(option)) },
              )
            }
          }
        }
      }

      val edited = panel.edited
      Section(
        title = Res.string.appearance_text_size,
        value = "${layout.fontScale}%",
        edited = edited.fontScale != null,
      ) {
        StepSlider(
          value = layout.fontScale,
          range = ReaderLayout.FontScaleRange,
          step = ReaderLayout.FONT_SCALE_STEP,
          onValueChange = { change(ReaderLayoutChange.FontScale(it)) },
          label = stringResource(Res.string.appearance_text_size),
          decreaseLabel = stringResource(Res.string.appearance_text_smaller),
          increaseLabel = stringResource(Res.string.appearance_text_larger),
          decreaseIcon = { Text("A", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
          increaseIcon = { Text("A", fontSize = 21.sp, fontWeight = FontWeight.SemiBold) },
        )
      }

      Section(Res.string.appearance_font, edited = edited.font != null) {
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ReaderFont.entries.forEach { font ->
            FilterChip(
              selected = layout.font == font,
              onClick = { change(ReaderLayoutChange.Font(font)) },
              label = { Text(stringResource(font.label)) },
            )
          }
        }
      }

      // In tenths, so the slider steps evenly
      val lineHeight = (layout.lineHeight * 10).roundToInt()
      Section(
        title = Res.string.appearance_line_spacing,
        value = "${lineHeight / 10}.${lineHeight % 10}",
        edited = edited.lineHeight != null,
      ) {
        StepSlider(
          value = lineHeight,
          range = LineHeightTenths,
          step = 1,
          onValueChange = { change(ReaderLayoutChange.LineHeight(it / 10f)) },
          label = stringResource(Res.string.appearance_line_spacing),
          decreaseLabel = stringResource(Res.string.appearance_line_spacing_less),
          increaseLabel = stringResource(Res.string.appearance_line_spacing_more),
          decreaseIcon = { Icon(CampfireIcons.Rounded.Remove, contentDescription = null) },
          increaseIcon = { Icon(CampfireIcons.Rounded.Add, contentDescription = null) },
        )
      }

      Section(Res.string.appearance_margins, edited = edited.margin != null) {
        Segmented(
          options = ReaderMargin.entries,
          selected = layout.margin,
          label = { it.label },
          onSelect = { change(ReaderLayoutChange.Margin(it)) },
        )
      }

      Section(Res.string.appearance_flow, edited = edited.flow != null) {
        Segmented(
          options = ReaderFlow.entries,
          selected = layout.flow,
          label = { it.label },
          onSelect = { change(ReaderLayoutChange.Flow(it)) },
        )
      }

      if (layout.flow == ReaderFlow.Paged) {
        Section(Res.string.appearance_pages, edited = edited.pages != null) {
          Segmented(
            options = ReaderPages.entries,
            selected = layout.pages,
            label = { it.label },
            onSelect = { change(ReaderLayoutChange.Pages(it)) },
          )
        }
      }

      SwitchRow(
        label = stringResource(Res.string.appearance_justify),
        checked = layout.justify,
        onCheckedChange = { change(ReaderLayoutChange.Justify(it)) },
      )
      SwitchRow(
        label = stringResource(Res.string.appearance_hyphenate),
        checked = layout.hyphenate,
        onCheckedChange = { change(ReaderLayoutChange.Hyphenate(it)) },
      )

      if (panel.scope == AppearanceScope.AllBooks) {
        SwitchRow(
          label = stringResource(Res.string.appearance_keep_screen_on),
          checked = keepScreenOn,
          onCheckedChange = { eventSink(ReaderUiEvent.KeepScreenOnChanged(it)) },
        )
        if (LocalWindowScreenController.current?.supportsVolumeKeys == true) {
          SwitchRow(
            label = stringResource(Res.string.appearance_volume_keys),
            detail = stringResource(Res.string.appearance_volume_keys_detail),
            checked = volumeKeysTurnPages,
            onCheckedChange = { eventSink(ReaderUiEvent.VolumeKeysChanged(it)) },
          )
        }

        // The theme and layout only: keeping the screen on and the volume keys are preferences
        OutlinedButton(
          onClick = { eventSink(ReaderUiEvent.ResetToDefaults) },
          enabled = theme != ReaderTheme.FollowApp || layout != ReaderLayout(),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(CampfireIcons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(Res.string.appearance_reset_defaults))
        }
      }
    }
  }
}

/** What the panel marks as this book's own: its overrides, while the panel edits this book. */
private val AppearancePanel.edited: ReaderLayoutOverrides
  get() = if (scope == AppearanceScope.ThisBook) overrides else ReaderLayoutOverrides.None

@Composable
private fun Section(
  title: StringResource,
  value: String? = null,
  edited: Boolean = false,
  content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (edited) {
        Text(
          text = stringResource(Res.string.appearance_this_book),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
      Spacer(Modifier.weight(1f))
      if (value != null) {
        Text(
          text = value,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
    content()
  }
}

/**
 * That this book reads with settings of its own: while the panel edits all books the page shows
 * them, so the book's own come back when the panel closes.
 */
@Composable
private fun OverridesNotice(
  scope: AppearanceScope,
  onReset: () -> Unit,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
  ) {
    val message = when (scope) {
      AppearanceScope.AllBooks -> Res.string.appearance_book_has_own
      AppearanceScope.ThisBook -> Res.string.appearance_book_edited
    }
    Text(
      text = stringResource(message),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f),
    )
    TextButton(onClick = onReset) {
      Text(stringResource(Res.string.appearance_reset_book))
    }
  }
}

@Composable
private fun ThemeSwatch(
  theme: ReaderTheme,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val palette = theme.palette()
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .padding(4.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(palette.background)
        .border(
          BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          ),
          CircleShape,
        ),
    ) {
      Text(
        text = "Aa",
        color = palette.foreground,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
      )
    }
    Text(
      text = stringResource(theme.label),
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

/**
 * A slider that moves in [step]s through [range], with buttons at either end that move one step.
 * The page follows every step as it's reached, rather than only once the slider is let go.
 */
@Composable
private fun StepSlider(
  value: Int,
  range: IntRange,
  step: Int,
  onValueChange: (Int) -> Unit,
  label: String,
  decreaseLabel: String,
  increaseLabel: String,
  decreaseIcon: @Composable () -> Unit,
  increaseIcon: @Composable () -> Unit,
) {
  // Follows the finger between steps; the value is what was last sent
  var dragging by remember { mutableStateOf<Float?>(null) }
  val steps = (range.last - range.first) / step
  val snap = { raw: Float -> (range.first + ((raw - range.first) / step).roundToInt() * step).coerceIn(range) }

  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButtonTooltip(text = decreaseLabel) {
      FilledTonalIconButton(
        onClick = { onValueChange((value - step).coerceIn(range)) },
        enabled = value > range.first,
      ) {
        Box(Modifier.semantics { contentDescription = decreaseLabel }) { decreaseIcon() }
      }
    }
    Slider(
      value = dragging ?: value.toFloat(),
      onValueChange = { raw ->
        dragging = raw
        val stepped = snap(raw)
        if (stepped != value) onValueChange(stepped)
      },
      onValueChangeFinished = { dragging = null },
      valueRange = range.first.toFloat()..range.last.toFloat(),
      steps = (steps - 1).coerceAtLeast(0),
      // A mark for every step crowds the track; the value beside the title says where it is
      track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, drawTick = { _, _ -> }) },
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 8.dp)
        .semantics { contentDescription = label },
    )
    IconButtonTooltip(text = increaseLabel) {
      FilledTonalIconButton(
        onClick = { onValueChange((value + step).coerceIn(range)) },
        enabled = value < range.last,
      ) {
        Box(Modifier.semantics { contentDescription = increaseLabel }) { increaseIcon() }
      }
    }
  }
}

@Composable
private fun <T> Segmented(
  options: List<T>,
  selected: T,
  label: (T) -> StringResource,
  onSelect: (T) -> Unit,
) {
  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    options.forEachIndexed { index, option ->
      SegmentedButton(
        selected = option == selected,
        onClick = { onSelect(option) },
        shape = SegmentedButtonDefaults.itemShape(index, options.size),
      ) {
        Text(stringResource(label(option)))
      }
    }
  }
}

@Composable
private fun SwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  detail: String? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
  ) {
    Column(Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.bodyLarge)
      if (detail != null) {
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = null,
      thumbContent = if (checked) {
        { Icon(CampfireIcons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
      } else {
        null
      },
    )
  }
}

private val AppearanceScope.label: StringResource
  get() = when (this) {
    AppearanceScope.AllBooks -> Res.string.appearance_all_books
    AppearanceScope.ThisBook -> Res.string.appearance_this_book
  }

private val ReaderFlow.label: StringResource
  get() = when (this) {
    ReaderFlow.Paged -> Res.string.appearance_flow_paged
    ReaderFlow.Scrolled -> Res.string.appearance_flow_scrolled
  }

private val ReaderTheme.label: StringResource
  get() = when (this) {
    ReaderTheme.FollowApp -> Res.string.appearance_theme_follow_app
    ReaderTheme.Light -> Res.string.appearance_theme_light
    ReaderTheme.Sepia -> Res.string.appearance_theme_sepia
    ReaderTheme.Dark -> Res.string.appearance_theme_dark
    ReaderTheme.Black -> Res.string.appearance_theme_black
  }

private val ReaderFont.label: StringResource
  get() = when (this) {
    ReaderFont.Publisher -> Res.string.appearance_font_publisher
    ReaderFont.Serif -> Res.string.appearance_font_serif
    ReaderFont.Sans -> Res.string.appearance_font_sans
    ReaderFont.Literata -> Res.string.appearance_font_literata
    ReaderFont.AtkinsonHyperlegible -> Res.string.appearance_font_atkinson
    ReaderFont.OpenDyslexic -> Res.string.appearance_font_opendyslexic
  }

private val ReaderMargin.label: StringResource
  get() = when (this) {
    ReaderMargin.Narrow -> Res.string.appearance_margin_narrow
    ReaderMargin.Normal -> Res.string.appearance_margin_normal
    ReaderMargin.Wide -> Res.string.appearance_margin_wide
  }

private val ReaderPages.label: StringResource
  get() = when (this) {
    ReaderPages.Auto -> Res.string.appearance_pages_auto
    ReaderPages.Single -> Res.string.appearance_pages_single
    ReaderPages.Two -> Res.string.appearance_pages_two
  }

/** Line spacing in tenths, so the slider steps evenly. */
private val LineHeightTenths =
  (ReaderLayout.LineHeightRange.start * 10).roundToInt()..(ReaderLayout.LineHeightRange.endInclusive * 10).roundToInt()
