// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.store

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.campfire.settings.app.KEY_ADAPT_TO_UNREACHABLE_SERVER
import app.campfire.settings.app.KEY_ANALYTICS_ID
import app.campfire.settings.app.KEY_ANALYTIC_REPORTING
import app.campfire.settings.app.KEY_APP_UPDATE_DISMISSED_VERSION_CODE
import app.campfire.settings.app.KEY_APP_UPDATE_SIGN_IN_DISMISSED
import app.campfire.settings.app.KEY_CRASH_REPORTING
import app.campfire.settings.app.KEY_CURRENT_USER_ID
import app.campfire.settings.app.KEY_DEVELOPER_MODE
import app.campfire.settings.app.KEY_DEVICE_ID
import app.campfire.settings.app.KEY_DOWNLOAD_ON_WIFI_ONLY
import app.campfire.settings.app.KEY_FAKE_APP_UPDATE_AVAILABLE
import app.campfire.settings.app.KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD
import app.campfire.settings.app.KEY_FAKE_APP_UPDATE_SIGNED_IN
import app.campfire.settings.app.KEY_HAS_CONSENTED
import app.campfire.settings.app.KEY_HLS_LARGE_ITEM_THRESHOLD
import app.campfire.settings.app.KEY_HOME_SERVER_ON_MOBILE_DATA
import app.campfire.settings.app.KEY_KEEP_SIGNED_IN_AFTER_REINSTALL
import app.campfire.settings.app.KEY_LAST_SEEN_WHATS_NEW
import app.campfire.settings.app.KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA
import app.campfire.settings.app.KEY_MEDIA_BUTTON_PACKAGES
import app.campfire.settings.app.KEY_SESSION_AGE
import app.campfire.settings.app.KEY_SHOW_WIDGET_PINNING
import app.campfire.settings.app.KEY_SOCKET_ENABLED
import app.campfire.settings.app.KEY_SUPPORTING_PANE_WIDTH
import app.campfire.settings.app.KEY_WIDE_NAVIGATION_RAIL_EXPANDED
import app.campfire.settings.library.KEY_AUTHORS_SORT_DIRECTION
import app.campfire.settings.library.KEY_AUTHOR_SORT_MODE
import app.campfire.settings.library.KEY_COLLECTIONS_DISPLAY_STATE
import app.campfire.settings.library.KEY_LIBRARY_ITEM_DISPLAY_STATE
import app.campfire.settings.library.KEY_LIBRARY_ITEM_MARQUEE
import app.campfire.settings.library.KEY_PLAYLISTS_DISPLAY_STATE
import app.campfire.settings.library.KEY_SERIES_DISPLAY_STATE
import app.campfire.settings.library.KEY_SERIES_SORT_DIRECTION
import app.campfire.settings.library.KEY_SERIES_SORT_MODE
import app.campfire.settings.library.KEY_SHOW_CONFIRM_DOWNLOAD
import app.campfire.settings.library.KEY_SHOW_TIME_IN_BOOK
import app.campfire.settings.library.KEY_SORT_DIRECTION
import app.campfire.settings.library.KEY_SORT_MODE
import app.campfire.settings.playback.KEY_AUTO_REWIND_AMOUNT
import app.campfire.settings.playback.KEY_AUTO_REWIND_ENABLED
import app.campfire.settings.playback.KEY_AUTO_SLEEP_END
import app.campfire.settings.playback.KEY_AUTO_SLEEP_START
import app.campfire.settings.playback.KEY_AUTO_SLEEP_TIMER
import app.campfire.settings.playback.KEY_AUTO_SLEEP_TIMER_ENABLED
import app.campfire.settings.playback.KEY_FADE_OUT_DURATION
import app.campfire.settings.playback.KEY_LAST_SET_SLEEP_TIMER
import app.campfire.settings.playback.KEY_SHAKE_SENSITIVITY
import app.campfire.settings.playback.KEY_SHAKE_TO_RESET
import app.campfire.settings.playback.PREF_AA_GRID_OVERRIDES
import app.campfire.settings.playback.PREF_AA_HIDDEN
import app.campfire.settings.playback.PREF_AA_ORDER
import app.campfire.settings.playback.PREF_AUTO_REWIND_ON_RESUME
import app.campfire.settings.playback.PREF_AUTO_REWIND_STOP_AT_CHAPTER
import app.campfire.settings.playback.PREF_AUTO_SYNC
import app.campfire.settings.playback.PREF_BACKWARD_TIME_MS
import app.campfire.settings.playback.PREF_BOOK_TIME_UI
import app.campfire.settings.playback.PREF_EQUALIZER_CUSTOM_GAINS
import app.campfire.settings.playback.PREF_EQUALIZER_PROFILE
import app.campfire.settings.playback.PREF_FORWARD_TIME_MS
import app.campfire.settings.playback.PREF_ITEM_EQUALIZER_PROFILES
import app.campfire.settings.playback.PREF_ITEM_PLAYBACK_SPEEDS
import app.campfire.settings.playback.PREF_MAX_RESUME_REWIND
import app.campfire.settings.playback.PREF_MIN_PAUSE_THRESHOLD
import app.campfire.settings.playback.PREF_MIN_RESUME_REWIND
import app.campfire.settings.playback.PREF_MP3_SEEKING
import app.campfire.settings.playback.PREF_OUTPUT_DEVICE
import app.campfire.settings.playback.PREF_OUTPUT_VOLUME
import app.campfire.settings.playback.PREF_PENDING_RESUME_REWIND
import app.campfire.settings.playback.PREF_PLAYBACK_HISTORY
import app.campfire.settings.playback.PREF_PLAYBACK_RATES
import app.campfire.settings.playback.PREF_PLAYBACK_SPEED
import app.campfire.settings.playback.PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS
import app.campfire.settings.playback.PREF_SCROLLING_TITLES
import app.campfire.settings.playback.PREF_STREAMING_METHOD
import app.campfire.settings.playback.PREF_SYNC
import app.campfire.settings.playback.PREF_SYNC_INTERVAL_METERED
import app.campfire.settings.playback.PREF_SYNC_INTERVAL_UNMETERED
import app.campfire.settings.playback.PREF_TRACK_RESET_THRESHOLD
import app.campfire.settings.playback.PREF_WAVY_SLIDER
import app.campfire.settings.theme.KEY_CURRENT_THEME
import app.campfire.settings.theme.KEY_ITEM_DETAIL_THEMING
import app.campfire.settings.theme.KEY_PLAYBACK_THEMING
import app.campfire.settings.theme.KEY_THEME
import com.russhwolf.settings.Settings

/**
 * Copies the settings out of the platform preferences they used to live in, once.
 *
 * The keys are listed with their types because DataStore keys are typed while the old storage can't always say
 * what it holds (desktop's java.util.prefs keeps everything as text). The old values are left in place for a
 * release, so a downgrade still finds them and a restored older backup still migrates.
 *
 * TODO: Delete along with the old values one release after DataStore ships (#607)
 */
class LegacySettingsMigration(
  private val legacy: Settings,
) : DataMigration<Preferences> {

  override suspend fun shouldMigrate(currentData: Preferences): Boolean =
    currentData[MIGRATED] != true

  override suspend fun migrate(currentData: Preferences): Preferences {
    val migrated = currentData.toMutablePreferences()
    LegacyBooleanKeys.forEach { key ->
      legacy.getBooleanOrNull(key)?.let { migrated[booleanPreferencesKey(key)] = it }
    }
    LegacyLongKeys.forEach { key ->
      legacy.getLongOrNull(key)?.let { migrated[longPreferencesKey(key)] = it }
    }
    LegacyFloatKeys.forEach { key ->
      legacy.getFloatOrNull(key)?.let { migrated[floatPreferencesKey(key)] = it }
    }
    LegacyDoubleKeys.forEach { key ->
      legacy.getDoubleOrNull(key)?.let { migrated[doublePreferencesKey(key)] = it }
    }
    LegacyStringKeys.forEach { key ->
      legacy.getStringOrNull(key)?.let { migrated[stringPreferencesKey(key)] = it }
    }
    migrated[MIGRATED] = true
    return migrated.toPreferences()
  }

  override suspend fun cleanUp() = Unit

  companion object {
    private val MIGRATED = booleanPreferencesKey("legacy_settings_migrated")
  }
}

internal val LegacyBooleanKeys = listOf(
  KEY_ADAPT_TO_UNREACHABLE_SERVER,
  KEY_ANALYTIC_REPORTING,
  KEY_APP_UPDATE_SIGN_IN_DISMISSED,
  KEY_AUTO_REWIND_ENABLED,
  KEY_AUTO_SLEEP_TIMER_ENABLED,
  KEY_CRASH_REPORTING,
  KEY_DEVELOPER_MODE,
  KEY_DOWNLOAD_ON_WIFI_ONLY,
  KEY_FAKE_APP_UPDATE_AVAILABLE,
  KEY_FAKE_APP_UPDATE_FAIL_DOWNLOAD,
  KEY_FAKE_APP_UPDATE_SIGNED_IN,
  KEY_HAS_CONSENTED,
  KEY_HOME_SERVER_ON_MOBILE_DATA,
  KEY_ITEM_DETAIL_THEMING,
  KEY_KEEP_SIGNED_IN_AFTER_REINSTALL,
  KEY_LEGACY_SKIP_HOME_SERVER_ON_MOBILE_DATA,
  KEY_LIBRARY_ITEM_MARQUEE,
  KEY_PLAYBACK_THEMING,
  KEY_SHAKE_TO_RESET,
  KEY_SHOW_CONFIRM_DOWNLOAD,
  KEY_SHOW_TIME_IN_BOOK,
  KEY_SHOW_WIDGET_PINNING,
  KEY_SOCKET_ENABLED,
  KEY_WIDE_NAVIGATION_RAIL_EXPANDED,
  PREF_AUTO_REWIND_ON_RESUME,
  PREF_AUTO_REWIND_STOP_AT_CHAPTER,
  PREF_AUTO_SYNC,
  PREF_BOOK_TIME_UI,
  PREF_MP3_SEEKING,
  PREF_PLAYBACK_HISTORY,
  PREF_REMOTE_NEXT_PREV_SKIPS_CHAPTERS,
  PREF_SCROLLING_TITLES,
  PREF_SYNC,
  PREF_WAVY_SLIDER,
)

internal val LegacyLongKeys = listOf(
  KEY_APP_UPDATE_DISMISSED_VERSION_CODE,
  PREF_BACKWARD_TIME_MS,
  PREF_FORWARD_TIME_MS,
)

internal val LegacyFloatKeys = listOf(
  KEY_SUPPORTING_PANE_WIDTH,
  PREF_OUTPUT_VOLUME,
  PREF_PLAYBACK_SPEED,
)

/** Durations, stored as seconds. */
internal val LegacyDoubleKeys = listOf(
  KEY_AUTO_REWIND_AMOUNT,
  KEY_FADE_OUT_DURATION,
  KEY_HLS_LARGE_ITEM_THRESHOLD,
  KEY_LAST_SET_SLEEP_TIMER,
  KEY_SESSION_AGE,
  PREF_MAX_RESUME_REWIND,
  PREF_MIN_PAUSE_THRESHOLD,
  PREF_MIN_RESUME_REWIND,
  PREF_SYNC_INTERVAL_METERED,
  PREF_SYNC_INTERVAL_UNMETERED,
  PREF_TRACK_RESET_THRESHOLD,
)

internal val LegacyStringKeys = listOf(
  KEY_ANALYTICS_ID,
  KEY_AUTHORS_SORT_DIRECTION,
  KEY_AUTHOR_SORT_MODE,
  KEY_AUTO_SLEEP_END,
  KEY_AUTO_SLEEP_START,
  KEY_AUTO_SLEEP_TIMER,
  KEY_COLLECTIONS_DISPLAY_STATE,
  KEY_CURRENT_THEME,
  KEY_CURRENT_USER_ID,
  KEY_DEVICE_ID,
  KEY_LAST_SEEN_WHATS_NEW,
  KEY_LIBRARY_ITEM_DISPLAY_STATE,
  KEY_MEDIA_BUTTON_PACKAGES,
  KEY_PLAYLISTS_DISPLAY_STATE,
  KEY_SERIES_DISPLAY_STATE,
  KEY_SERIES_SORT_DIRECTION,
  KEY_SERIES_SORT_MODE,
  KEY_SHAKE_SENSITIVITY,
  KEY_SORT_DIRECTION,
  KEY_SORT_MODE,
  KEY_THEME,
  PREF_AA_GRID_OVERRIDES,
  PREF_AA_HIDDEN,
  PREF_AA_ORDER,
  PREF_EQUALIZER_CUSTOM_GAINS,
  PREF_EQUALIZER_PROFILE,
  PREF_ITEM_EQUALIZER_PROFILES,
  PREF_ITEM_PLAYBACK_SPEEDS,
  PREF_OUTPUT_DEVICE,
  PREF_PENDING_RESUME_REWIND,
  PREF_PLAYBACK_RATES,
  PREF_STREAMING_METHOD,
)
