// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.android.plugin.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.livewire.ui.actions.ClickAction
import com.livewire.ui.graphics.CircleShape
import com.livewire.ui.graphics.RoundedCornerShape
import com.livewire.ui.layout.Alignment
import com.livewire.ui.layout.Arrangement
import com.livewire.ui.layout.Column
import com.livewire.ui.layout.Row
import com.livewire.ui.layout.RowScope
import com.livewire.ui.modifier.LivewireModifier
import com.livewire.ui.modifier.fillMaxWidth
import com.livewire.ui.modifier.height
import com.livewire.ui.modifier.padding
import com.livewire.ui.theme.LivewireTheme
import com.livewire.ui.widget.Button
import com.livewire.ui.widget.ButtonShapes
import com.livewire.ui.widget.ButtonSize
import com.livewire.ui.widget.ButtonStyle
import com.livewire.ui.widget.HorizontalDivider
import com.livewire.ui.widget.ProgressIndicator
import com.livewire.ui.widget.ProgressIndicatorStyle
import com.livewire.ui.widget.Spacer
import com.livewire.ui.widget.Surface
import com.livewire.ui.widget.Text

/**
 * Shared building blocks for Campfire's Livewire debug plugins.
 */

@Composable
internal fun SectionHeader(
  title: String,
  modifier: LivewireModifier = LivewireModifier,
  trailingAction: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier
      .height(56.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = LivewireTheme.typography.titleMedium,
      modifier = LivewireModifier.weight(1f),
    )
    trailingAction()
  }
}

/**
 * An iOS-settings-style segment: a header followed by a rounded card of
 * label/value rows separated by dividers.
 */
@Composable
internal fun SegmentedSection(
  title: String,
  rows: List<Pair<String, String>>,
) {
  if (title.isNotEmpty()) SectionHeader(title)
  Surface(
    modifier = LivewireModifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    tonalElevation = 3.dp,
  ) {
    Column(LivewireModifier.fillMaxWidth()) {
      rows.forEachIndexed { index, (label, value) ->
        if (index > 0) HorizontalDivider()
        Row(
          LivewireModifier
            .fillMaxWidth()
            .padding(12.dp),
        ) {
          Text(
            text = label,
            modifier = LivewireModifier.weight(1f),
            color = Color.Gray,
            style = LivewireTheme.typography.bodyMedium,
          )
          Text(
            text = value,
            style = LivewireTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}

/**
 * The standard loading state: a centered circular spinner with a short label
 * beneath it. Size via [modifier] — fillMaxSize() for whole-pane loads.
 */
@Composable
internal fun LoadingIndicator(
  message: String,
  modifier: LivewireModifier = LivewireModifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ProgressIndicator(style = ProgressIndicatorStyle.Circular)
    Spacer(LivewireModifier.height(8.dp))
    Text(
      text = message,
      style = LivewireTheme.typography.bodySmall,
      color = Color.Gray,
    )
  }
}

/**
 * The standard section action button: extra-small, tonal, rounded-rect that
 * rounds to a circle when pressed. Use for Clear / Pause all / Resume all etc.
 */
@Composable
internal fun SectionButton(
  action: ClickAction,
  modifier: LivewireModifier = LivewireModifier,
  content: @Composable RowScope.() -> Unit,
) {
  Button(
    action = action,
    modifier = modifier,
    size = ButtonSize.ExtraSmall,
    style = ButtonStyle.Tonal,
    shapes = ButtonShapes(
      shape = RoundedCornerShape(8.dp),
      pressedShape = CircleShape,
    ),
    content = content,
  )
}

/**
 * A compact two-line log entry: timestamp + color-coded type (+ optional source)
 * on the header line, details underneath.
 */
@Composable
internal fun LogRow(
  time: String,
  type: String,
  typeColor: Color,
  source: String?,
  details: String,
) {
  Column(
    LivewireModifier
      .fillMaxWidth()
      .padding(
        vertical = 4.dp,
      ),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = time,
        style = LivewireTheme.typography.labelMedium,
        color = Color.Gray,
      )
      Spacer(LivewireModifier.padding(4.dp))
      Text(
        text = type,
        style = LivewireTheme.typography.labelMedium,
        color = typeColor,
      )
      if (source != null) {
        Spacer(LivewireModifier.padding(4.dp))
        Text(
          text = source,
          style = LivewireTheme.typography.labelMedium,
          color = Color.LightGray,
        )
      }
    }
    Text(
      text = details,
      style = LivewireTheme.typography.bodySmall,
    )
  }
}
