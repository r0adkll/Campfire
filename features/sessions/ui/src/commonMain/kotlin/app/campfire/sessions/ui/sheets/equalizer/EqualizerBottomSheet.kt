// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.Changed
import app.campfire.analytics.events.Equalizer
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.ScreenType
import app.campfire.analytics.events.ScreenViewEvent
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.profileOrNull
import app.campfire.common.compose.analytics.Impression
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Book
import app.campfire.common.compose.icons.rounded.Globe
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.VerticalSlider
import app.campfire.common.compose.widgets.bottomSheetShape
import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerPresets
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.AppScope
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.LibraryItemId
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.settings.api.EqualizerSettings
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.equalizer_band_gain_cd
import campfire.features.sessions.ui.generated.resources.equalizer_bass_label
import campfire.features.sessions.ui.generated.resources.equalizer_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.equalizer_enabled_toggle
import campfire.features.sessions.ui.generated.resources.equalizer_loudness_label
import campfire.features.sessions.ui.generated.resources.equalizer_per_book_toggle
import campfire.features.sessions.ui.generated.resources.equalizer_preset_bass_boost
import campfire.features.sessions.ui.generated.resources.equalizer_preset_custom
import campfire.features.sessions.ui.generated.resources.equalizer_preset_flat
import campfire.features.sessions.ui.generated.resources.equalizer_preset_podcast
import campfire.features.sessions.ui.generated.resources.equalizer_preset_reduce_noise
import campfire.features.sessions.ui.generated.resources.equalizer_preset_treble_boost
import campfire.features.sessions.ui.generated.resources.equalizer_preset_voice_boost
import campfire.features.sessions.ui.generated.resources.equalizer_preset_warm
import campfire.features.sessions.ui.generated.resources.equalizer_unavailable_casting
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jetbrains.compose.resources.stringResource

data class EqualizerInput(
  val itemId: LibraryItemId,
)

suspend fun OverlayHost.showEqualizerBottomSheet(
  itemId: LibraryItemId,
) {
  show(
    BottomSheetOverlay(
      model = EqualizerInput(itemId),
      onDismiss = { },
      sheetShape = bottomSheetShape,
      skipPartiallyExpandedState = true,
    ) { input, _ ->
      Impression {
        ScreenViewEvent("Equalizer", ScreenType.Overlay)
      }

      EqualizerBottomSheet(
        input = input,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@ContributesTo(AppScope::class)
interface EqualizerBottomSheetComponent {
  val equalizerSettings: EqualizerSettings
  val audioPlayerHolder: AudioPlayerHolder
}

@Composable
private fun rememberEqualizerComponent(): EqualizerBottomSheetComponent {
  return remember {
    ComponentHolder.component<EqualizerBottomSheetComponent>()
  }
}

/**
 * State/DI layer for the equalizer sheet: resolves the component, collects the player and
 * settings flows, and handles events so [EqualizerSheet] stays pure and previewable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun EqualizerBottomSheet(
  input: EqualizerInput,
  modifier: Modifier = Modifier,
  component: EqualizerBottomSheetComponent = rememberEqualizerComponent(),
) {
  val equalizerState by remember {
    component.audioPlayerHolder.currentPlayer
      .flatMapLatest {
        it?.equalizer ?: emptyFlow()
      }
  }.collectAsState(EqualizerState.Available(component.equalizerSettings.equalizerProfileFor(input.itemId)))

  val itemEqualizerProfiles by remember {
    component.equalizerSettings.observeItemEqualizerProfiles()
  }.collectAsState()

  val profile = equalizerState.profileOrNull ?: EqualizerProfile()

  val pushProfile: (EqualizerProfile) -> Unit = { updated ->
    component.audioPlayerHolder.currentPlayer.value
      ?.setEqualizer(updated)
      ?: component.equalizerSettings.setEqualizerProfileFor(input.itemId, updated)
  }

  EqualizerSheet(
    profile = profile,
    available = equalizerState is EqualizerState.Available,
    perBookEnabled = input.itemId in itemEqualizerProfiles,
    onEnabledChange = { enabled ->
      Analytics.send(PlaybackActionEvent(Equalizer, Changed, extras = mapOf("enabled" to enabled)))
      pushProfile(profile.copy(enabled = enabled))
    },
    onPresetSelected = { presetId ->
      Analytics.send(PlaybackActionEvent(Equalizer, Changed, extras = mapOf("preset" to presetId)))
      val gains = if (presetId == EqualizerPresets.CUSTOM_ID) {
        component.equalizerSettings.customBandGains
      } else {
        EqualizerPresets.forId(presetId)?.bandGainsDb ?: profile.bandGainsDb
      }
      pushProfile(profile.copy(presetId = presetId, bandGainsDb = gains))
    },
    onBandGainChange = { index, gainDb ->
      val gains = profile.bandGainsDb.toMutableList().also { it[index] = gainDb }
      // Touching any fader turns the profile into the persisted "Custom" curve
      component.equalizerSettings.customBandGains = gains
      pushProfile(profile.copy(presetId = EqualizerPresets.CUSTOM_ID, bandGainsDb = gains))
    },
    onLoudnessChange = { loudnessGainDb ->
      pushProfile(profile.copy(loudnessGainDb = loudnessGainDb))
    },
    onBassBoostChange = { bassBoost ->
      pushProfile(profile.copy(bassBoost = bassBoost))
    },
    onPerBookChange = { enabled ->
      Analytics.send(PlaybackActionEvent(Equalizer, Changed, extras = mapOf("perBook" to enabled)))
      if (enabled) {
        component.equalizerSettings.itemEqualizerProfiles += (input.itemId to profile)
      } else {
        component.equalizerSettings.itemEqualizerProfiles -= input.itemId
        // Snap active playback back to the global profile the item now falls back to
        component.audioPlayerHolder.currentPlayer.value
          ?.setEqualizer(component.equalizerSettings.equalizerProfile)
      }
    },
    modifier = modifier,
  )
}

@Composable
internal fun EqualizerSheet(
  profile: EqualizerProfile,
  available: Boolean,
  perBookEnabled: Boolean,
  onEnabledChange: (Boolean) -> Unit,
  onPresetSelected: (String) -> Unit,
  onBandGainChange: (Int, Float) -> Unit,
  onLoudnessChange: (Float) -> Unit,
  onBassBoostChange: (Float) -> Unit,
  onPerBookChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val controlsEnabled = available && profile.enabled

  SessionSheetLayout(
    modifier = modifier,
    title = { Text(stringResource(Res.string.equalizer_bottomsheet_title)) },
    trailingContent = {
      Switch(
        checked = perBookEnabled,
        onCheckedChange = onPerBookChange,
        enabled = available,
        thumbContent = {
          Icon(
            imageVector = if (perBookEnabled) {
              CampfireIcons.Rounded.Book
            } else {
              CampfireIcons.Rounded.Globe
            },
            modifier = Modifier.size(16.dp),
            contentDescription = stringResource(Res.string.equalizer_per_book_toggle),
          )
        },
        modifier = Modifier.align(Alignment.CenterEnd),
      )
    },
  ) {
    if (!available) {
      Text(
        text = stringResource(Res.string.equalizer_unavailable_casting),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
      )
      Spacer(Modifier.height(8.dp))
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Text(
        text = stringResource(Res.string.equalizer_enabled_toggle),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.weight(1f),
      )
      Switch(
        checked = profile.enabled,
        onCheckedChange = onEnabledChange,
        enabled = available,
      )
    }

    Spacer(Modifier.height(8.dp))

    PresetChipRow(
      selectedPresetId = profile.presetId,
      enabled = controlsEnabled,
      onPresetSelected = onPresetSelected,
    )

    Spacer(Modifier.height(16.dp))

    BandFaderRow(
      bandGainsDb = profile.bandGainsDb,
      enabled = controlsEnabled,
      onBandGainChange = onBandGainChange,
    )

    Spacer(Modifier.height(16.dp))

    LabeledSlider(
      label = stringResource(Res.string.equalizer_loudness_label),
      value = profile.loudnessGainDb,
      valueRange = EqualizerBands.LoudnessGainRangeDb,
      valueText = "+${profile.loudnessGainDb.roundToInt()} dB",
      enabled = controlsEnabled,
      onValueChange = { onLoudnessChange(it.roundToInt().toFloat()) },
    )

    LabeledSlider(
      label = stringResource(Res.string.equalizer_bass_label),
      value = profile.bassBoost,
      valueRange = EqualizerBands.BassBoostRange,
      valueText = "${(profile.bassBoost * 100).roundToInt()}%",
      enabled = controlsEnabled,
      onValueChange = { onBassBoostChange((it * 20).roundToInt() / 20f) },
    )

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun PresetChipRow(
  selectedPresetId: String,
  enabled: Boolean,
  onPresetSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val presetIds = remember { EqualizerPresets.all.map { it.id } + EqualizerPresets.CUSTOM_ID }
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp),
  ) {
    presetIds.forEach { presetId ->
      FilterChip(
        selected = presetId == selectedPresetId,
        enabled = enabled,
        onClick = { onPresetSelected(presetId) },
        label = { Text(presetLabel(presetId)) },
      )
    }
  }
}

@Composable
private fun BandFaderRow(
  bandGainsDb: List<Float>,
  enabled: Boolean,
  onBandGainChange: (Int, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
  ) {
    EqualizerBands.centerFrequenciesHz.forEachIndexed { index, frequencyHz ->
      BandFader(
        frequencyHz = frequencyHz,
        gainDb = bandGainsDb.getOrElse(index) { 0f },
        enabled = enabled,
        onGainChange = { onBandGainChange(index, it) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun BandFader(
  frequencyHz: Int,
  gainDb: Float,
  enabled: Boolean,
  onGainChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
  ) {
    // Local state keeps the drag smooth; rounding to whole dB means nearby drag frames
    // resolve to the same value, and only real changes are pushed
    var draggedGain by remember(gainDb) { mutableStateOf(gainDb) }

    Text(
      text = draggedGain.asGainLabel(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
    )
    VerticalSlider(
      value = draggedGain,
      valueRange = EqualizerBands.BandGainRangeDb,
      enabled = enabled,
      onValueChange = { raw ->
        val gain = raw.roundToInt().toFloat()
        if (gain != draggedGain) {
          draggedGain = gain
          onGainChange(gain)
        }
      },
      modifier = Modifier
        .height(BandFaderHeight)
        .width(32.dp)
        .bandSemantics(frequencyHz),
    )
    Text(
      text = frequencyHz.asFrequencyLabel(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun LabeledSlider(
  label: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  valueText: String,
  enabled: Boolean,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.width(72.dp),
    )
    Slider(
      value = value,
      valueRange = valueRange,
      enabled = enabled,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = valueText,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.width(56.dp),
    )
  }
}

@Composable
private fun presetLabel(presetId: String): String = when (presetId) {
  EqualizerPresets.FLAT_ID -> stringResource(Res.string.equalizer_preset_flat)
  EqualizerPresets.VOICE_BOOST_ID -> stringResource(Res.string.equalizer_preset_voice_boost)
  EqualizerPresets.PODCAST_ID -> stringResource(Res.string.equalizer_preset_podcast)
  EqualizerPresets.BASS_BOOST_ID -> stringResource(Res.string.equalizer_preset_bass_boost)
  EqualizerPresets.TREBLE_BOOST_ID -> stringResource(Res.string.equalizer_preset_treble_boost)
  EqualizerPresets.WARM_ID -> stringResource(Res.string.equalizer_preset_warm)
  EqualizerPresets.REDUCE_NOISE_ID -> stringResource(Res.string.equalizer_preset_reduce_noise)
  else -> stringResource(Res.string.equalizer_preset_custom)
}

@Composable
private fun Modifier.bandSemantics(frequencyHz: Int): Modifier {
  // The rotated slider keeps Material slider semantics; label it with its band so
  // screen readers can distinguish the ten faders
  val label = stringResource(Res.string.equalizer_band_gain_cd, frequencyHz.asFrequencyLabel())
  return semantics { contentDescription = label }
}

private fun Float.asGainLabel(): String {
  val rounded = roundToInt()
  return if (rounded > 0) "+$rounded" else "$rounded"
}

private fun Int.asFrequencyLabel(): String {
  return if (this >= 1000) "${this / 1000}k" else toString()
}

private val BandFaderHeight = 160.dp

@Preview
@Composable
private fun EqualizerSheetPreview() {
  CampfireTheme {
    var profile by remember {
      mutableStateOf(
        EqualizerProfile(
          enabled = true,
          presetId = EqualizerPresets.VOICE_BOOST_ID,
          bandGainsDb = EqualizerPresets.VoiceBoost.bandGainsDb,
          loudnessGainDb = 6f,
          bassBoost = 0.25f,
        ),
      )
    }
    var perBook by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
      sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded),
      sheetContent = {
        EqualizerSheet(
          profile = profile,
          available = true,
          perBookEnabled = perBook,
          onEnabledChange = { profile = profile.copy(enabled = it) },
          onPresetSelected = { presetId ->
            val gains = EqualizerPresets.forId(presetId)?.bandGainsDb ?: profile.bandGainsDb
            profile = profile.copy(presetId = presetId, bandGainsDb = gains)
          },
          onBandGainChange = { index, gain ->
            profile = profile.copy(
              presetId = EqualizerPresets.CUSTOM_ID,
              bandGainsDb = profile.bandGainsDb.toMutableList().also { it[index] = gain },
            )
          },
          onLoudnessChange = { profile = profile.copy(loudnessGainDb = it) },
          onBassBoostChange = { profile = profile.copy(bassBoost = it) },
          onPerBookChange = { perBook = it },
        )
      },
    ) {
    }
  }
}
