// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.session.UserSession
import app.campfire.db.test.testDb
import app.campfire.settings.api.ReaderFlow
import app.campfire.settings.api.ReaderFont
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderMargin
import app.campfire.settings.api.ReaderPages
import app.campfire.user.test.fixtures.user
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class DatabaseReaderBookSettingsRepositoryTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  @Test
  fun overrides_roundTrip_andNoneRemovesThem() = testDb { db ->
    val repository = DatabaseReaderBookSettingsRepository(UserSession.LoggedIn(user(id = "user-1")), db, dispatchers)
    val overrides = ReaderLayoutOverrides(
      font = ReaderFont.Literata,
      fontScale = 130,
      lineHeight = 1.8f,
      margin = ReaderMargin.Wide,
      justify = false,
      hyphenate = true,
      flow = ReaderFlow.Scrolled,
      pages = ReaderPages.Two,
    )

    assertThat(repository.observeOverrides("item-1").first()).isEqualTo(ReaderLayoutOverrides.None)

    repository.setOverrides("item-1", overrides)
    assertThat(repository.observeOverrides("item-1").first()).isEqualTo(overrides)
    assertThat(repository.observeOverrides("item-2").first()).isEqualTo(ReaderLayoutOverrides.None)

    repository.setOverrides("item-1", ReaderLayoutOverrides.None)
    assertThat(repository.observeOverrides("item-1").first()).isEqualTo(ReaderLayoutOverrides.None)
  }
}
