// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.settings.api.DownloadSettings
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
@ContributesBinding(AppScope::class, binding = binding<DownloadSettings>())
@Inject
class DownloadSettingsImpl(
  @ForScope(AppScope::class) override val settings: ObservableSettings,
  @ForScope(AppScope::class) override val scope: CoroutineScope,
) : DownloadSettings, AppSettings() {

  private val downloadEpisodesOnPlayOrQueueProperty = booleanSetting(KEY_DOWNLOAD_EPISODES_ON_PLAY_OR_QUEUE, false)
  override var downloadEpisodesOnPlayOrQueue: Boolean by downloadEpisodesOnPlayOrQueueProperty
  override fun observeDownloadEpisodesOnPlayOrQueue(): StateFlow<Boolean> =
    downloadEpisodesOnPlayOrQueueProperty.observe()

  private val deleteFinishedEpisodesProperty = booleanSetting(KEY_DELETE_FINISHED_EPISODES, false)
  override var deleteFinishedEpisodes: Boolean by deleteFinishedEpisodesProperty
  override fun observeDeleteFinishedEpisodes(): StateFlow<Boolean> = deleteFinishedEpisodesProperty.observe()
}

// Named for when the setting only covered queued episodes, kept so it stays on for those who turned it on
internal const val KEY_DOWNLOAD_EPISODES_ON_PLAY_OR_QUEUE = "pref_download_queued_episodes"
internal const val KEY_DELETE_FINISHED_EPISODES = "pref_delete_finished_episodes"
