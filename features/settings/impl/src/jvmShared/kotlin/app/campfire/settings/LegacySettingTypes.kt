// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

/** How a setting was read before FlatPrefs, and so the type it's stored as now. */
internal enum class LegacySettingType { Boolean, Int, Long, Float, Double, String }

/**
 * The type of every setting stored before FlatPrefs, for the old values that can't say: desktop's
 * java.util.prefs kept everything as text, and multiplatform-settings kept doubles in
 * SharedPreferences as a long holding their bits.
 *
 * This is a snapshot of what existed when the migration was written. Settings added later are
 * stored with their type from the start and don't belong here.
 */
internal object LegacySettingTypes {

  fun typeOf(key: String): LegacySettingType? =
    exact[key] ?: prefixes.firstOrNull { (prefix, _) -> key.startsWith(prefix) }?.second

  val doubleKeys: Set<String> get() = exact.filterValues { it == LegacySettingType.Double }.keys

  private val exact: Map<String, LegacySettingType> = buildMap {
    listOf(
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
    ).forEach { put(it, LegacySettingType.Boolean) }

    listOf(
      KEY_APP_UPDATE_DISMISSED_VERSION_CODE,
      PREF_BACKWARD_TIME_MS,
      PREF_FORWARD_TIME_MS,
    ).forEach { put(it, LegacySettingType.Long) }

    listOf(
      KEY_SUPPORTING_PANE_WIDTH,
      PREF_OUTPUT_VOLUME,
      PREF_PLAYBACK_SPEED,
    ).forEach { put(it, LegacySettingType.Float) }

    // Durations, in seconds
    listOf(
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
    ).forEach { put(it, LegacySettingType.Double) }

    listOf(
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
      // AccountRestoreStore
      "account_restore_snapshot",
    ).forEach { put(it, LegacySettingType.String) }
  }

  // Keys other modules build from a user, library or provider id
  private val prefixes: List<Pair<String, LegacySettingType>> = listOf(
    // DiscoverScanStateStore
    "discover_scan_at_" to LegacySettingType.Long,
    "discover_scan_skipped_" to LegacySettingType.Int,
    "discover_scan_failed_" to LegacySettingType.Int,
    "discover_scan_rate_limited_" to LegacySettingType.Boolean,
    // RemovedLibraryItemReconciler
    "library_items_reconciled_at_" to LegacySettingType.Long,
    // DefaultBookInfoProviderSettings
    "bookinfo_enabled_" to LegacySettingType.Boolean,
    "bookinfo_preferred_" to LegacySettingType.String,
    "bookinfo_series_missing_" to LegacySettingType.Boolean,
  )
}
