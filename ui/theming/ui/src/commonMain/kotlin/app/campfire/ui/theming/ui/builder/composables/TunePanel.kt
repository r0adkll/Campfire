// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.builder.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.asHct
import app.campfire.ui.theming.api.ThemeRole
import app.campfire.ui.theming.api.colorFor
import app.campfire.ui.theming.ui.builder.color.ContrastCheck
import app.campfire.ui.theming.ui.builder.color.TEXT_CONTRAST
import app.campfire.ui.theming.ui.builder.color.checks
import app.campfire.ui.theming.ui.builder.color.contrastRatio
import app.campfire.ui.theming.ui.builder.color.roles
import app.campfire.ui.theming.ui.preview.DarkModeSwitch
import campfire.ui.theming.ui.generated.resources.Res
import campfire.ui.theming.ui.generated.resources.theme_fine_tune_dark_mode
import campfire.ui.theming.ui.generated.resources.theme_picker_colorfulness
import campfire.ui.theming.ui.generated.resources.theme_picker_done
import campfire.ui.theming.ui.generated.resources.theme_picker_hex
import campfire.ui.theming.ui.generated.resources.theme_picker_hue
import campfire.ui.theming.ui.generated.resources.theme_tune_also_changes
import campfire.ui.theming.ui.generated.resources.theme_tune_background
import campfire.ui.theming.ui.generated.resources.theme_tune_borders
import campfire.ui.theming.ui.generated.resources.theme_tune_button
import campfire.ui.theming.ui.generated.resources.theme_tune_easy_to_see
import campfire.ui.theming.ui.generated.resources.theme_tune_editing_dark
import campfire.ui.theming.ui.generated.resources.theme_tune_editing_light
import campfire.ui.theming.ui.generated.resources.theme_tune_fix
import campfire.ui.theming.ui.generated.resources.theme_tune_foreground
import campfire.ui.theming.ui.generated.resources.theme_tune_hard_to_read
import campfire.ui.theming.ui.generated.resources.theme_tune_hard_to_see
import campfire.ui.theming.ui.generated.resources.theme_tune_lightness
import campfire.ui.theming.ui.generated.resources.theme_tune_mirror_to_dark
import campfire.ui.theming.ui.generated.resources.theme_tune_mirror_to_light
import campfire.ui.theming.ui.generated.resources.theme_tune_needs
import campfire.ui.theming.ui.generated.resources.theme_tune_only_here
import campfire.ui.theming.ui.generated.resources.theme_tune_progress
import campfire.ui.theming.ui.generated.resources.theme_tune_ratio
import campfire.ui.theming.ui.generated.resources.theme_tune_readable
import campfire.ui.theming.ui.generated.resources.theme_tune_reset
import campfire.ui.theming.ui.generated.resources.theme_tune_track
import campfire.ui.theming.ui.generated.resources.theme_tune_unchecked
import campfire.ui.theming.ui.generated.resources.theme_tune_used_by
import com.r0adkll.swatchbuckler.color.hct.Hct
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_HUE = 360f
private const val MAX_CHROMA = 120f
private const val MAX_TONE = 100f

/**
 * Sets exact colors for one part of the app in one mode: the part's background and foreground,
 * how readable they are together, and a fix when they aren't. From the All colors list there's
 * no part, just [role], and no readability meter.
 *
 * Unlike the palette picker, this has a lightness slider: here the user sets the exact shade.
 *
 * @param scheme the finished scheme for the mode being edited, fine-tunes included.
 * @param overrides the roles fine-tuned in the mode being edited.
 * @param showModeSwitch whether to offer switching modes here. On a phone the preview's own
 * switch does that, and the panel follows it.
 */
@Composable
internal fun TunePanel(
  check: ContrastCheck?,
  role: ThemeRole,
  scheme: ColorScheme,
  overrides: Map<ThemeRole, Color>,
  dark: Boolean,
  showModeSwitch: Boolean,
  onDarkChange: (Boolean) -> Unit,
  onRoleChange: (ThemeRole) -> Unit,
  onColorChange: (Color) -> Unit,
  onFixContrastClick: () -> Unit,
  onResetClick: () -> Unit,
  onMirrorClick: () -> Unit,
  onDoneClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val color = scheme.colorFor(role)
  val roles = check?.roles ?: listOf(role)
  val isTuned = roles.any { it in overrides }

  // Keyed on the role and mode, so switching either starts the sliders from that color.
  val sliders = remember(role, dark) { HctSliders(color) }
  // Set when a button here changes the color, so the sliders follow it. Otherwise the color only
  // changes because the sliders or hex field moved it, and they keep what the user set.
  var followColor by remember { mutableStateOf(false) }
  LaunchedEffect(color, followColor) {
    if (followColor) {
      sliders.reset(color)
      followColor = false
    }
  }

  Column(modifier) {
    // The part, its colors and how readable they are stay in sight while the sliders below move.
    TunePanelHeader(
      check = check,
      role = role,
      scheme = scheme,
      dark = dark,
      showModeSwitch = showModeSwitch,
      onDarkChange = onDarkChange,
      onRoleChange = onRoleChange,
      onFixContrastClick = {
        onFixContrastClick()
        followColor = true
      },
      onDoneClick = onDoneClick,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )

    TuneControls(
      check = check,
      role = role,
      color = color,
      sliders = sliders,
      isTuned = isTuned,
      dark = dark,
      onColorChange = onColorChange,
      onResetClick = {
        onResetClick()
        followColor = true
      },
      onMirrorClick = onMirrorClick,
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    )
  }
}

@Composable
private fun TunePanelHeader(
  check: ContrastCheck?,
  role: ThemeRole,
  scheme: ColorScheme,
  dark: Boolean,
  showModeSwitch: Boolean,
  onDarkChange: (Boolean) -> Unit,
  onRoleChange: (ThemeRole) -> Unit,
  onFixContrastClick: () -> Unit,
  onDoneClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text(
          text = check?.label() ?: role.tokenName,
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = stringResource(if (dark) Res.string.theme_tune_editing_dark else Res.string.theme_tune_editing_light),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (showModeSwitch) {
        DarkModeSwitch(
          isDarkMode = dark,
          onDarkModeChange = onDarkChange,
          label = stringResource(Res.string.theme_fine_tune_dark_mode),
          modifier = Modifier.padding(end = 8.dp),
        )
      }
      FilledTonalButton(onClick = onDoneClick) {
        Text(stringResource(Res.string.theme_picker_done))
      }
    }

    if (check != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoleButton(
          label = stringResource(check.backgroundLabel),
          role = check.background,
          color = scheme.colorFor(check.background),
          selected = role == check.background,
          onClick = { onRoleChange(check.background) },
          modifier = Modifier.weight(1f),
        )
        RoleButton(
          label = stringResource(check.foregroundLabel),
          role = check.foreground,
          color = scheme.colorFor(check.foreground),
          selected = role == check.foreground,
          onClick = { onRoleChange(check.foreground) },
          modifier = Modifier.weight(1f),
        )
      }
      ContrastMeter(
        check = check,
        ratio = contrastRatio(scheme.colorFor(check.foreground), scheme.colorFor(check.background)),
        onFixClick = onFixContrastClick,
      )
    }
  }
}

/** The sliders, hex field and actions: the part of the panel that scrolls. */
@Composable
private fun TuneControls(
  check: ContrastCheck?,
  role: ThemeRole,
  color: Color,
  sliders: HctSliders,
  isTuned: Boolean,
  dark: Boolean,
  onColorChange: (Color) -> Unit,
  onResetClick: () -> Unit,
  onMirrorClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentOnColorChange by rememberUpdatedState(onColorChange)
  LaunchedEffect(sliders) {
    snapshotFlow { sliders.hex.text.toString() }.collect { text ->
      val typed = text.parseHexColor() ?: return@collect
      if (typed != sliders.color) {
        sliders.reset(typed, updateHex = false)
        currentOnColorChange(typed)
      }
    }
  }

  Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    GradientSlider(
      label = stringResource(Res.string.theme_picker_hue),
      value = sliders.hue,
      maxValue = MAX_HUE,
      trackColors = List(13) { colorOf(it * 30.0, maxOf(sliders.chroma, 30f).toDouble(), sliders.tone.toDouble()) },
      onValueChange = { onColorChange(sliders.set(hue = it)) },
    )
    GradientSlider(
      label = stringResource(Res.string.theme_picker_colorfulness),
      value = sliders.chroma,
      maxValue = MAX_CHROMA,
      trackColors = List(7) { colorOf(sliders.hue.toDouble(), it * 20.0, sliders.tone.toDouble()) },
      onValueChange = { onColorChange(sliders.set(chroma = it)) },
    )
    GradientSlider(
      label = stringResource(Res.string.theme_tune_lightness),
      value = sliders.tone,
      maxValue = MAX_TONE,
      trackColors = List(9) { colorOf(sliders.hue.toDouble(), sliders.chroma.toDouble(), it * 12.5) },
      onValueChange = { onColorChange(sliders.set(tone = it)) },
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      ColorDot(color, size = 40.dp)
      Spacer(Modifier.width(12.dp))
      OutlinedTextField(
        state = sliders.hex,
        label = { Text(stringResource(Res.string.theme_picker_hex)) },
        prefix = { Text("#") },
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.maxLength(6),
        modifier = Modifier.width(160.dp),
      )
    }

    Text(
      text = sharedWith(check, role),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = onResetClick, enabled = isTuned) {
        Text(stringResource(Res.string.theme_tune_reset))
      }
      TextButton(onClick = onMirrorClick, enabled = isTuned) {
        Text(
          stringResource(if (dark) Res.string.theme_tune_mirror_to_light else Res.string.theme_tune_mirror_to_dark),
        )
      }
    }
  }
}

@Composable
private fun RoleButton(
  label: String,
  role: ThemeRole,
  color: Color,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    selected = selected,
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = if (selected) colors.surfaceContainerHigh else colors.surfaceContainer,
    border = if (selected) BorderStroke(2.dp, colors.primary) else BorderStroke(1.dp, colors.outlineVariant),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        Modifier
          .size(28.dp)
          .background(color, RoundedCornerShape(8.dp))
          .border(1.dp, colors.onSurface.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
      )
      Column(Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = role.tokenName,
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = colors.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/** How readable the part is now, what it needs, and a fix when it falls short. */
@Composable
private fun ContrastMeter(
  check: ContrastCheck,
  ratio: Double,
  onFixClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val passes = ratio >= check.minimumRatio
  val isText = check.minimumRatio >= TEXT_CONTRAST
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = colors.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 48.dp)
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(Res.string.theme_tune_ratio, ratio.ratioText()),
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = stringResource(
          when {
            isText && passes -> Res.string.theme_tune_readable
            isText -> Res.string.theme_tune_hard_to_read
            passes -> Res.string.theme_tune_easy_to_see
            else -> Res.string.theme_tune_hard_to_see
          },
        ),
        style = MaterialTheme.typography.labelMedium,
        color = if (passes) colors.onPrimaryContainer else colors.onErrorContainer,
        modifier = Modifier
          .background(if (passes) colors.primaryContainer else colors.errorContainer, RoundedCornerShape(50))
          .padding(horizontal = 8.dp, vertical = 2.dp),
      )
      Text(
        text = stringResource(Res.string.theme_tune_needs, check.minimumRatio.ratioText()),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier.weight(1f),
      )
      if (!passes) {
        FilledTonalButton(onClick = onFixClick) {
          Text(stringResource(Res.string.theme_tune_fix))
        }
      }
    }
  }
}

/** Which other parts a change to [role] reaches. */
@Composable
private fun sharedWith(check: ContrastCheck?, role: ThemeRole): String {
  val others = role.checks.filter { it != check }.map { it.label() }
  return when {
    others.isNotEmpty() && check != null -> {
      stringResource(Res.string.theme_tune_also_changes, others.joinToString(", "))
    }
    others.isNotEmpty() -> stringResource(Res.string.theme_tune_used_by, others.joinToString(", "))
    check != null -> stringResource(Res.string.theme_tune_only_here)
    else -> stringResource(Res.string.theme_tune_unchecked)
  }
}

private val ContrastCheck.backgroundLabel: StringResource
  get() = when (this) {
    ContrastCheck.Buttons -> Res.string.theme_tune_button
    ContrastCheck.ProgressBars -> Res.string.theme_tune_track
    else -> Res.string.theme_tune_background
  }

private val ContrastCheck.foregroundLabel: StringResource
  get() = when (this) {
    ContrastCheck.ProgressBars -> Res.string.theme_tune_progress
    ContrastCheck.Borders -> Res.string.theme_tune_borders
    else -> Res.string.theme_tune_foreground
  }

/** The role's name in Material's color tokens, such as `surfaceContainerHigh`. */
internal val ThemeRole.tokenName: String get() = name.replaceFirstChar { it.lowercase() }

/**
 * Hue, colorfulness and lightness for the color being tuned, plus its hex code. The sliders keep
 * what the user set even where that hue can't reach the colorfulness asked for, so dragging
 * through a dull stretch doesn't lose the setting.
 */
@Stable
private class HctSliders(initial: Color) {
  var hue by mutableFloatStateOf(0f)
    private set
  var chroma by mutableFloatStateOf(0f)
    private set
  var tone by mutableFloatStateOf(0f)
    private set

  /** The color the sliders last made or were reset to. */
  var color by mutableStateOf(initial)
    private set

  val hex = TextFieldState(initial.hexCode().drop(1))

  init {
    reset(initial, updateHex = false)
  }

  fun reset(color: Color, updateHex: Boolean = true) {
    val hct = color.asHct()
    hue = hct.hue.toFloat()
    chroma = hct.chroma.toFloat()
    tone = hct.tone.toFloat()
    this.color = color
    if (updateHex) hex.setTextAndPlaceCursorAtEnd(color.hexCode().drop(1))
  }

  fun set(hue: Float = this.hue, chroma: Float = this.chroma, tone: Float = this.tone): Color {
    this.hue = hue
    this.chroma = chroma
    this.tone = tone
    color = Color(Hct.from(hue.toDouble(), chroma.toDouble(), tone.toDouble()).toInt())
    hex.setTextAndPlaceCursorAtEnd(color.hexCode().drop(1))
    return color
  }
}
