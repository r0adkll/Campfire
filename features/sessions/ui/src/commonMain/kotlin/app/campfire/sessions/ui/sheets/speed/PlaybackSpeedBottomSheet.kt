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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Book
import app.campfire.common.compose.icons.rounded.Globe
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.PlaybackSpeedDialog
import app.campfire.common.compose.widgets.PlaybackSpeedRange
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.extensions.readableHundredths
import app.campfire.core.extensions.roundToHundredths
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.settings.api.PlaybackSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.speed_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.speed_custom_open
import campfire.features.sessions.ui.generated.resources.speed_per_book_toggle
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jetbrains.compose.resources.stringResource

data class PlaybackSpeedInput(
  val itemId: LibraryItemId,
  val speed: Float,
)

suspend fun OverlayHost.showPlaybackSpeedBottomSheet(
  itemId: LibraryItemId,
  speed: Float,
) {
  show(
    BottomSheetOverlay(
      model = PlaybackSpeedInput(itemId, speed),
      onDismiss = { },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      skipPartiallyExpandedState = true,
    ) { input, _ ->
      Impression {
        ScreenViewEvent("PlaybackSpeed", ScreenType.Overlay)
      }

      PlaybackSpeedBottomSheet(
        input = input,
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

/**
 * State/DI layer for the playback speed sheet: resolves the component, collects the player and
 * settings flows, and handles events so [PlaybackSpeedSheet] stays pure and previewable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun PlaybackSpeedBottomSheet(
  input: PlaybackSpeedInput,
  modifier: Modifier = Modifier,
  component: PlaybackSpeedBottomSheetComponent = rememberPlaybackSpeedComponent(),
) {
  val currentSpeed by remember {
    component.audioPlayerHolder.currentPlayer
      .flatMapLatest {
        it?.playbackSpeed ?: emptyFlow()
      }
  }.collectAsState(input.speed)

  val speedOptions = remember { component.playbackSettings.playbackRates.distinct().sorted() }

  val itemPlaybackSpeeds by remember {
    component.playbackSettings.observeItemPlaybackSpeeds()
  }.collectAsState()

  PlaybackSpeedSheet(
    currentSpeed = currentSpeed,
    speedOptions = speedOptions,
    perBookSpeedEnabled = input.itemId in itemPlaybackSpeeds,
    onSpeedChange = { speed ->
      Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("speed" to speed)))
      component.audioPlayerHolder.currentPlayer.value
        ?.setPlaybackSpeed(speed)
    },
    onPerBookSpeedChange = { enabled ->
      Analytics.send(PlaybackActionEvent(Speed, Changed, extras = mapOf("perBook" to enabled)))
      if (enabled) {
        component.playbackSettings.itemPlaybackSpeeds += (input.itemId to currentSpeed)
      } else {
        component.playbackSettings.itemPlaybackSpeeds -= input.itemId
        // Snap active playback back to the global speed the item now falls back to
        component.audioPlayerHolder.currentPlayer.value
          ?.setPlaybackSpeed(component.playbackSettings.playbackSpeed)
      }
    },
    modifier = modifier,
  )
}

@Composable
internal fun PlaybackSpeedSheet(
  currentSpeed: Float,
  speedOptions: List<Float>,
  perBookSpeedEnabled: Boolean,
  onSpeedChange: (Float) -> Unit,
  onPerBookSpeedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.speed_bottomsheet_title)) },
    trailingContent = {
      Switch(
        checked = perBookSpeedEnabled,
        onCheckedChange = onPerBookSpeedChange,
        thumbContent = {
          Icon(
            imageVector = if (perBookSpeedEnabled) {
              CampfireIcons.Rounded.Book
            } else {
              CampfireIcons.Rounded.Globe
            },
            modifier = Modifier.size(16.dp),
            contentDescription = stringResource(Res.string.speed_per_book_toggle),
          )
        },
        modifier = Modifier.align(Alignment.CenterEnd),
      )
    },
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
          label = { Text("${defaultSpeed.readableHundredths}x") },
          onClick = { onSpeedChange(defaultSpeed) },
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
      var sliderValue by remember { mutableStateOf(currentSpeed) }

      LaunchedEffect(currentSpeed) {
        if (sliderValue != currentSpeed) {
          sliderValue = currentSpeed
        }
      }

      val sliderProgressNormalized = sliderValue / (PlaybackSpeedRange.start + PlaybackSpeedRange.length)
      val waveLength = lerp(WavelengthRange.endInclusive, WavelengthRange.start, sliderProgressNormalized)
      val waveHeight = lerp(WaveHeightRange.start, WaveHeightRange.endInclusive, sliderProgressNormalized)
      val waveVelocity = lerp(WaveVelocityRange.start, WaveVelocityRange.endInclusive, sliderProgressNormalized)
      val waveThickness = lerp(WaveThicknessRange.start, WaveThicknessRange.endInclusive, sliderProgressNormalized)

      val setSpeed: (Float) -> Unit = { raw ->
        val speed = raw.roundToHundredths().coerceIn(PlaybackSpeedRange)
        // Rounding to hundredths means nearby drag frames resolve to the same value; only push real changes
        if (speed != sliderValue) {
          sliderValue = speed
          onSpeedChange(speed)
        }
      }

      WavySlider(
        value = sliderValue,
        valueRange = PlaybackSpeedRange,
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
        PlaybackSpeedDialog(
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

@Preview
@Composable
private fun PlaybackSpeedSheetPreview() {
  CampfireTheme {
    var speed by remember { mutableStateOf(1.25f) }
    var perBook by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
      sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
      sheetContent = {
        PlaybackSpeedSheet(
          currentSpeed = speed,
          speedOptions = listOf(1f, 1.1f, 1.25f, 1.5f, 2f),
          perBookSpeedEnabled = perBook,
          onSpeedChange = { speed = it },
          onPerBookSpeedChange = { perBook = it },
        )
      },
    ) {
    }
  }
}

private val WavelengthRange = 40.dp.rangeTo(135.dp)
private val WaveHeightRange = 0.dp.rangeTo(40.dp)
private val WaveVelocityRange = 50.dp.rangeTo(120.dp)
private val WaveThicknessRange = 16.dp.rangeTo(6.dp)

private val ClosedFloatingPointRange<Float>.length: Float get() = endInclusive - start
