// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.campfire.settings.DataStoreSettings
import app.campfire.settings.LegacySettingsMigration
import com.russhwolf.settings.SharedPreferencesSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Times loading the settings DataStore the way startup does: opening it and reading it once, with and without
 * migrating the settings saved in SharedPreferences by earlier versions.
 *
 * ```
 * ./gradlew :features:settings:benchmark:connectedReleaseAndroidTest
 * ```
 */
@RunWith(AndroidJUnit4::class)
class SettingsLoadBenchmark {

  @get:Rule
  val benchmarkRule = BenchmarkRule()

  private val context: Context = InstrumentationRegistry.getInstrumentation().context
  private val file = File(context.filesDir, "datastore/settings.preferences_pb")
  private val legacy = SharedPreferencesSettings(context.getSharedPreferences("legacy_settings", Context.MODE_PRIVATE))

  @Before
  fun setUp() {
    file.delete()
    legacy.clear()
    seedLegacySettings()
  }

  /** Every launch after the first: the file exists and has already been migrated. */
  @Test
  fun load() {
    // Migrate once so every measured load finds the file already migrated
    loadOnce()
    benchmarkRule.measureRepeated { loadOnce(measure = this) }
  }

  /** The first launch after updating: the file doesn't exist yet, so the old settings are copied into it. */
  @Test
  fun loadWithMigration() {
    benchmarkRule.measureRepeated {
      runWithMeasurementDisabled { file.delete() }
      loadOnce(measure = this)
    }
  }

  /**
   * Opens and loads the store as `SettingsDataStoreComponent` does, then closes it outside the measurement:
   * DataStore allows one open store per file, so the next iteration can only open it once this one is closed.
   */
  private fun loadOnce(measure: BenchmarkRule.Scope? = null) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val dataStore = PreferenceDataStoreFactory.createWithPath(
      corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
      migrations = listOf(LegacySettingsMigration(legacy)),
      scope = scope,
      produceFile = { file.toOkioPath() },
    )
    runBlocking { DataStoreSettings(dataStore, scope).load() }

    val close = { runBlocking { scope.coroutineContext.job.cancelAndJoin() } }
    if (measure != null) measure.runWithMeasurementDisabled(close) else close()
  }

  /** The settings a real install had changed before updating, captured from a device. */
  private fun seedLegacySettings() = with(legacy) {
    putBoolean("pref_has_consented", true)
    putBoolean("pref_show_confirm_download", false)
    putBoolean("pref_playback_mp3_seeking", true)
    putBoolean("pref_sleep_shake_to_reset", true)
    putBoolean("pref_dynamically_theme_item_detail", false)
    putBoolean("pref_dynamically_theme_playback", false)
    putBoolean("pref_sleep_auto_timer_enabled", true)
    putBoolean("pref_show_widget_pinning", true)
    putBoolean("pref_library_item_marquee", false)
    putBoolean("pref_playback_remote_next_prev_skips_chapters", true)
    putBoolean("pref_download_on_wifi_only", true)
    putLong("pref_playback_backward_time_ms", 30_000L)
    putLong("pref_playback_forward_time_ms", 5_000L)
    putDouble("pref_sleep_fade_out_duration", 37.0)
    putString("pref_android_auto_grid_overrides", "series=true")
    putString("pref_android_auto_hidden_categories", "shows")
    putString("pref_current_theme", "mountain")
    putString("pref_theme", "dark")
    putString("pref_sort_mode", "duration")
    putString("pref_sort_direction", "desc")
    putString("pref_last_seen_whats_new", "1.2.1")
    putString("pref_playback_rates", "1.0::1.7::1.25::1.5::2.0")
  }
}
