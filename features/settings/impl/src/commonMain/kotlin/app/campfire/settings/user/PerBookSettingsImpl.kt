// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.user

import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.settings.api.EqualizerSettings
import app.campfire.settings.api.PerBookSettings
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.playback.asItemProfileMap
import app.campfire.settings.playback.serializeProfiles
import app.campfire.settings.store.AppSettings
import app.campfire.settings.store.SettingsStore
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@SingleIn(UserScope::class)
@ContributesBinding(UserScope::class, binding = binding<PerBookSettings>())
@Inject
class PerBookSettingsImpl(
  @UserSettings override val store: SettingsStore,
  private val playbackSettings: PlaybackSettings,
  private val equalizerSettings: EqualizerSettings,
) : PerBookSettings, AppSettings() {

  private val itemPlaybackSpeedsProperty = customSetting(
    key = PREF_ITEM_PLAYBACK_SPEEDS,
    defaultValue = emptyMap<LibraryItemId, Float>(),
    getter = { it.asItemSpeedMap() },
    setter = { speeds ->
      speeds.entries.joinToString(ITEM_SPEED_ENTRY_SEPARATOR) { "${it.key}$ITEM_SPEED_VALUE_SEPARATOR${it.value}" }
    },
  )
  override fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>) = itemPlaybackSpeedsProperty.set(value)
  override fun observeItemPlaybackSpeeds(): Flow<Map<LibraryItemId, Float>> = itemPlaybackSpeedsProperty.observe()

  override suspend fun playbackSpeedFor(itemId: LibraryItemId?): Float =
    itemId?.let { itemPlaybackSpeedsProperty.get()[it] } ?: playbackSettings.observePlaybackSpeed().first()

  // Decided in this account's write order; the app-wide speed is written from here, so it keeps that order
  override fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float) = edit { preferences ->
    val speeds = itemPlaybackSpeedsProperty.readFrom(preferences)
    if (itemId != null && itemId in speeds) {
      itemPlaybackSpeedsProperty.writeTo(preferences, speeds + (itemId to speed))
    } else {
      playbackSettings.setPlaybackSpeed(speed)
    }
  }

  private val itemEqualizerProfilesProperty = customSetting(
    key = PREF_ITEM_EQUALIZER_PROFILES,
    defaultValue = emptyMap<LibraryItemId, EqualizerProfile>(),
    getter = { it.asItemProfileMap() },
    setter = { profiles -> profiles.serializeProfiles() },
  )
  override fun setItemEqualizerProfiles(value: Map<LibraryItemId, EqualizerProfile>) =
    itemEqualizerProfilesProperty.set(value)
  override fun observeItemEqualizerProfiles(): Flow<Map<LibraryItemId, EqualizerProfile>> =
    itemEqualizerProfilesProperty.observe()

  override suspend fun equalizerProfileFor(itemId: LibraryItemId?): EqualizerProfile =
    itemId?.let { itemEqualizerProfilesProperty.get()[it] } ?: equalizerSettings.observeEqualizerProfile().first()

  override fun setEqualizerProfileFor(itemId: LibraryItemId?, profile: EqualizerProfile) = edit { preferences ->
    val profiles = itemEqualizerProfilesProperty.readFrom(preferences)
    if (itemId != null && itemId in profiles) {
      itemEqualizerProfilesProperty.writeTo(preferences, profiles + (itemId to profile))
    } else {
      equalizerSettings.setEqualizerProfile(profile)
    }
  }

  private fun String.asItemSpeedMap(): Map<LibraryItemId, Float> {
    return split(ITEM_SPEED_ENTRY_SEPARATOR)
      .mapNotNull { entry ->
        val itemId = entry.substringBefore(ITEM_SPEED_VALUE_SEPARATOR)
        val speed = entry.substringAfter(ITEM_SPEED_VALUE_SEPARATOR, "").toFloatOrNull()
        if (itemId.isEmpty() || speed == null) null else itemId to speed
      }
      .toMap()
  }
}

internal const val PREF_ITEM_PLAYBACK_SPEEDS = "pref_item_playback_speeds"
internal const val PREF_ITEM_EQUALIZER_PROFILES = "pref_item_equalizer_profiles"
private const val ITEM_SPEED_ENTRY_SEPARATOR = "::"
private const val ITEM_SPEED_VALUE_SEPARATOR = "|"
