// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.speed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Changed
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.analytics.events.Speed
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.common.compose.analytics.Impression
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.extensions.readable
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.extensions.roundToHundredths
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.settings.api.PlaybackSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.speed_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.speed_custom_action_apply
import campfire.features.sessions.ui.generated.resources.speed_custom_action_cancel
import campfire.features.sessions.ui.generated.resources.speed_custom_dialog_error
import campfire.features.sessions.ui.generated.resources.speed_custom_dialog_label
import campfire.features.sessions.ui.generated.resources.speed_custom_dialog_title
import campfire.features.sessions.ui.generated.resources.speed_custom_open
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jetbrains.compose.resources.stringResource

suspend fun OverlayHost.showPlaybackSpeedBottomSheet(speed: Float) {
  show(
    BottomSheetOverlay(
      model = speed,
      onDismiss = { },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { s, _ ->
      Impression {
        ScreenViewEvent("PlaybackSpeed", ScreenType.Overlay)
      }

      PlaybackSpeedBottomSheet(
        speed = s,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@ContributesTo(AppScope::class)
interface PlaybackSpeedBottomSheetComponent {
  val playbackSettings: PlaybackSettings
  val audioPlayerHolder: AudioPlayerHolder
}

@Composable
private fun rememberPlaybackSpeedComponent(): PlaybackSpeedBottomSheetComponent {
  return remember {
    ComponentHolder.component<PlaybackSpeedBottomSheetComponent>()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun PlaybackSpeedBottomSheet(
  speed: Float,
  modifier: Modifier = Modifier,
  component: PlaybackSpeedBottomSheetComponent = rememberPlaybackSpeedComponent(),
) {
  val currentSpeed by remember {
    component.audioPlayerHolder.currentPlayer
      .flatMapLatest {
        it?.playbackSpeed ?: emptyFlow()
      }
  }.collectAsState(speed)

  val speedOptions = remember { component.playbackSettings.playbackRates.sorted() }

  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.speed_bottomsheet_title)) },
  ) {
    Spacer(Modifier.height(16.dp))

    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(horizontal = 16.dp),
    ) {
      speedOptions.forEachIndexed { index, defaultSpeed ->
        val isCurrentSpeed = currentSpeed == defaultSpeed
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index, speedOptions.size, RoundedCornerShape(16.dp)),
          selected = isCurrentSpeed,
          label = { Text("${defaultSpeed.readable}x") },
          onClick = {
            Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("speed" to defaultSpeed)))
            component.audioPlayerHolder.currentPlayer.value
              ?.setPlaybackSpeed(defaultSpeed)
          },
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .height(56.dp)
        .fillMaxSize()
        .padding(horizontal = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      var sliderValue by remember { mutableStateOf(speed) }

      LaunchedEffect(currentSpeed) {
        if (sliderValue != currentSpeed) {
          sliderValue = currentSpeed
        }
      }

      val sliderProgressNormalized = sliderValue / (DefaultSpeedRange.start + DefaultSpeedRange.length)
      val waveLength = lerp(WavelengthRange.endInclusive, WavelengthRange.start, sliderProgressNormalized)
      val waveHeight = lerp(WaveHeightRange.start, WaveHeightRange.endInclusive, sliderProgressNormalized)
      val waveVelocity = lerp(WaveVelocityRange.start, WaveVelocityRange.endInclusive, sliderProgressNormalized)
      val waveThickness = lerp(WaveThicknessRange.start, WaveThicknessRange.endInclusive, sliderProgressNormalized)

      val setSpeed: (Float) -> Unit = { raw ->
        val speed = raw.roundToHundredths().coerceIn(DefaultSpeedRange)
        // Rounding to hundredths means nearby drag frames resolve to the same value; only push real changes
        if (speed != sliderValue) {
          sliderValue = speed
          Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("speed" to speed)))
          component.audioPlayerHolder.currentPlayer.value?.setPlaybackSpeed(speed)
        }
      }

      WavySlider(
        value = sliderValue,
        valueRange = DefaultSpeedRange,
        onValueChange = setSpeed,
        waveLength = waveLength,
        waveHeight = waveHeight,
        waveVelocity = waveVelocity to WaveDirection.TAIL,
        waveThickness = waveThickness,
        incremental = true,
        modifier = Modifier.weight(1f),
      )

      var showCustomSpeedDialog by remember { mutableStateOf(false) }
      val customSpeedLabel = stringResource(Res.string.speed_custom_open)

      Text(
        text = "${sliderValue.readableHundredths}x",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
          .padding(start = 12.dp)
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClickLabel = customSpeedLabel) { showCustomSpeedDialog = true }
          .padding(horizontal = 8.dp, vertical = 12.dp)
          .width(44.dp),
      )

      if (showCustomSpeedDialog) {
        CustomSpeedDialog(
          initialSpeed = sliderValue,
          onDismiss = { showCustomSpeedDialog = false },
          onConfirm = { speed ->
            showCustomSpeedDialog = false
            setSpeed(speed)
          },
        )
      }
    }

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun CustomSpeedDialog(
  initialSpeed: Float,
  onDismiss: () -> Unit,
  onConfirm: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  var input by remember { mutableStateOf(initialSpeed.readableHundredths) }
  val parsed = parsePlaybackSpeed(input)
  val isError = input.isNotBlank() && parsed == null
  val rangeError = stringResource(
    Res.string.speed_custom_dialog_error,
    DefaultSpeedRange.start.readable,
    DefaultSpeedRange.endInclusive.readable,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier,
    title = { Text(stringResource(Res.string.speed_custom_dialog_title)) },
    text = {
      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        isError = isError,
        label = { Text(stringResource(Res.string.speed_custom_dialog_label)) },
        suffix = { Text("x") },
        supportingText = { Text(rangeError) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      )
    },
    confirmButton = {
      TextButton(
        enabled = parsed != null,
        onClick = { parsed?.let(onConfirm) },
      ) {
        Text(stringResource(Res.string.speed_custom_action_apply))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.speed_custom_action_cancel))
      }
    },
  )
}

/**
 * Parse a user-entered playback speed such as "1.2", "1,2" or "1.2x", rounded to hundredths.
 * Returns null if the text isn't a number or falls outside [DefaultSpeedRange].
 */
internal fun parsePlaybackSpeed(text: String): Float? {
  val value = text.trim()
    .removeSuffix("x")
    .removeSuffix("X")
    .trim()
    .replace(',', '.')
    .toFloatOrNull()
    ?.roundToHundredths()
    ?: return null
  return value.takeIf { it in DefaultSpeedRange }
}

private val WavelengthRange = 40.dp.rangeTo(135.dp)
private val WaveHeightRange = 0.dp.rangeTo(40.dp)
private val WaveVelocityRange = 50.dp.rangeTo(120.dp)
private val WaveThicknessRange = 16.dp.rangeTo(6.dp)

internal val DefaultSpeedRange = 0.5f.rangeTo(2f)
private val ClosedFloatingPointRange<Float>.length: Float get() = endInclusive - start
