// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.settings

import app.campfire.core.coroutines.CoroutineScopeHolder
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

class DeleteFilesByDefaultTest {

  private val scope = CoroutineScope(Dispatchers.Unconfined + Job())

  @AfterTest
  fun tearDown() {
    scope.cancel()
  }

  private fun settings(storage: MapSettings) = LibraryViewSettingsImpl(
    settings = storage,
    scopeHolder = CoroutineScopeHolder { scope },
  )

  @Test
  fun `deleting starts with the files chosen until there's a choice to remember`() {
    val settings = settings(MapSettings())

    assertThat(settings.deleteItemFilesByDefault).isTrue()
    assertThat(settings.deleteEpisodeFilesByDefault).isTrue()
  }

  @Test
  fun `items and episodes remember their own last choice`() {
    val storage = MapSettings()
    val settings = settings(storage)

    settings.deleteEpisodeFilesByDefault = false

    val reopened = settings(storage)
    assertThat(reopened.deleteEpisodeFilesByDefault).isFalse()
    assertThat(reopened.deleteItemFilesByDefault).isTrue()
    assertThat(settings.observeDeleteEpisodeFilesByDefault().value).isFalse()
    assertThat(settings.observeDeleteItemFilesByDefault().value).isTrue()
  }
}
