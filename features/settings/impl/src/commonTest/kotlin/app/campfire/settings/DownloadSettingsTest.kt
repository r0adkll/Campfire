// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class DownloadSettingsTest {

  private val scope = CoroutineScope(Dispatchers.Unconfined + Job())

  @AfterTest
  fun tearDown() {
    scope.cancel()
  }

  @Test
  fun `both default to off`() {
    val settings = DownloadSettingsImpl(MapSettings(), scope)

    assertThat(settings.downloadEpisodesOnPlayOrQueue).isFalse()
    assertThat(settings.deleteFinishedEpisodes).isFalse()
  }

  @Test
  fun `turning them on persists and is observed`() {
    val storage = MapSettings()
    val settings = DownloadSettingsImpl(storage, scope)

    settings.downloadEpisodesOnPlayOrQueue = true
    settings.deleteFinishedEpisodes = true

    val reopened = DownloadSettingsImpl(storage, scope)
    assertThat(reopened.downloadEpisodesOnPlayOrQueue).isTrue()
    assertThat(reopened.deleteFinishedEpisodes).isTrue()
    assertThat(settings.observeDownloadEpisodesOnPlayOrQueue().value).isTrue()
    assertThat(settings.observeDeleteFinishedEpisodes().value).isTrue()
  }
}
