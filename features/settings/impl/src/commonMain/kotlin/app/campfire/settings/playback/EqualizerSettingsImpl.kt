// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.playback

import app.campfire.core.audio.EqualizerBands
import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.store.AppSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalSettingsApi::class)
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<EqualizerSettings>())
@Inject
class EqualizerSettingsImpl(
  override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : EqualizerSettings, AppSettings() {

  private val equalizerProfileProperty = customSetting(
    key = PREF_EQUALIZER_PROFILE,
    defaultValue = EqualizerProfile(),
    getter = { it.asEqualizerProfile() ?: EqualizerProfile() },
    setter = { profile -> profile.serialize() },
  )
  override var equalizerProfile: EqualizerProfile by equalizerProfileProperty
  override fun observeEqualizerProfile(): StateFlow<EqualizerProfile> = equalizerProfileProperty.observe()

  override var customBandGains: List<Float> by customSetting(
    key = PREF_EQUALIZER_CUSTOM_GAINS,
    defaultValue = List(EqualizerBands.BAND_COUNT) { 0f },
    getter = { it.asBandGains() ?: List(EqualizerBands.BAND_COUNT) { 0f } },
    setter = { gains -> gains.joinToString(EQUALIZER_GAINS_SEPARATOR) },
  )

  private val itemEqualizerProfilesProperty = customSetting(
    key = PREF_ITEM_EQUALIZER_PROFILES,
    defaultValue = emptyMap<LibraryItemId, EqualizerProfile>(),
    getter = { it.asItemProfileMap() },
    setter = { profiles ->
      profiles.entries.joinToString(EQUALIZER_ENTRY_SEPARATOR) {
        "${it.key}$EQUALIZER_FIELD_SEPARATOR${it.value.serialize()}"
      }
    },
  )
  override var itemEqualizerProfiles: Map<LibraryItemId, EqualizerProfile> by itemEqualizerProfilesProperty
  override fun observeItemEqualizerProfiles(): StateFlow<Map<LibraryItemId, EqualizerProfile>> =
    itemEqualizerProfilesProperty.observe()

  private fun EqualizerProfile.serialize(): String = listOf(
    if (enabled) "1" else "0",
    presetId,
    bandGainsDb.joinToString(EQUALIZER_GAINS_SEPARATOR),
    loudnessGainDb.toString(),
    bassBoost.toString(),
  ).joinToString(EQUALIZER_FIELD_SEPARATOR)

  private fun String.asEqualizerProfile(): EqualizerProfile? {
    val fields = split(EQUALIZER_FIELD_SEPARATOR)
    if (fields.size != 5) return null
    val (enabled, presetId, gains, loudness, bass) = fields
    return EqualizerProfile(
      enabled = enabled == "1",
      presetId = presetId.ifEmpty { return null },
      bandGainsDb = gains.asBandGains() ?: return null,
      loudnessGainDb = loudness.toFloatOrNull() ?: return null,
      bassBoost = bass.toFloatOrNull() ?: return null,
    )
  }

  private fun String.asBandGains(): List<Float>? {
    val gains = split(EQUALIZER_GAINS_SEPARATOR).mapNotNull { it.toFloatOrNull() }
    return gains.takeIf { it.size == EqualizerBands.BAND_COUNT }
  }

  private fun String.asItemProfileMap(): Map<LibraryItemId, EqualizerProfile> {
    return split(EQUALIZER_ENTRY_SEPARATOR)
      .mapNotNull { entry ->
        val itemId = entry.substringBefore(EQUALIZER_FIELD_SEPARATOR)
        val profile = entry.substringAfter(EQUALIZER_FIELD_SEPARATOR, "").asEqualizerProfile()
        if (itemId.isEmpty() || profile == null) null else itemId to profile
      }
      .toMap()
  }
}

internal const val PREF_EQUALIZER_PROFILE = "pref_equalizer_profile"
internal const val PREF_EQUALIZER_CUSTOM_GAINS = "pref_equalizer_custom_gains"
internal const val PREF_ITEM_EQUALIZER_PROFILES = "pref_item_equalizer_profiles"
internal const val EQUALIZER_ENTRY_SEPARATOR = "::"
internal const val EQUALIZER_FIELD_SEPARATOR = "|"
internal const val EQUALIZER_GAINS_SEPARATOR = ","
