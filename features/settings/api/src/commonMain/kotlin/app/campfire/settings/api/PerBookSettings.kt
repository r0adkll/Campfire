// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.api

import app.campfire.core.audio.EqualizerProfile
import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow

/**
 * The playback speed and equalizer the signed-in account keeps for particular books, layered over the
 * app-wide [PlaybackSettings] and [EqualizerSettings]. Bound in the user graph, so each account has its own.
 */
interface PerBookSettings {

  /**
   * Per-book playback speeds, keyed by library item id. An entry means the book has its own speed;
   * books without one use the app-wide speed.
   */
  fun setItemPlaybackSpeeds(value: Map<LibraryItemId, Float>)
  fun observeItemPlaybackSpeeds(): Flow<Map<LibraryItemId, Float>>

  /** The speed [itemId] plays at: its own speed if it has one, otherwise the app-wide speed. */
  suspend fun playbackSpeedFor(itemId: LibraryItemId?): Float

  /** Saves [speed] as [itemId]'s own speed if it has one, otherwise as the app-wide speed. */
  fun setPlaybackSpeedFor(itemId: LibraryItemId?, speed: Float)

  /**
   * Per-book equalizer profiles, keyed by library item id. An entry means the book has its own
   * equalizer; books without one use the app-wide profile.
   */
  fun setItemEqualizerProfiles(value: Map<LibraryItemId, EqualizerProfile>)
  fun observeItemEqualizerProfiles(): Flow<Map<LibraryItemId, EqualizerProfile>>

  /** The equalizer [itemId] plays with: its own profile if it has one, otherwise the app-wide profile. */
  suspend fun equalizerProfileFor(itemId: LibraryItemId?): EqualizerProfile

  /** Saves [profile] as [itemId]'s own profile if it has one, otherwise as the app-wide profile. */
  fun setEqualizerProfileFor(itemId: LibraryItemId?, profile: EqualizerProfile)
}
