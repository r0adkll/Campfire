// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader

import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.db.test.testDb
import app.campfire.reader.api.EbookContentsEntry
import app.campfire.reader.api.entryAt
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class DatabaseEbookContentsRepositoryTest {

  private val dispatchers = DispatcherProvider(
    io = Dispatchers.Default,
    databaseWrite = Dispatchers.Default,
    databaseRead = Dispatchers.Default,
    computation = Dispatchers.Default,
    main = Dispatchers.Default,
  )

  private val prologue = EbookContentsEntry("Prologue", "text/prologue.xhtml", depth = 0, fraction = 0f)
  private val one = EbookContentsEntry("Chapter 1", "text/ch1.xhtml", depth = 0, fraction = 0.1f)
  private val oneA = EbookContentsEntry("Part A", "text/ch1.xhtml#a", depth = 1, fraction = null)

  @Test
  fun contents_keepTheirOrder_andSavingAgainReplacesThem() = testDb { db ->
    val repository = DatabaseEbookContentsRepository(db, dispatchers)

    assertThat(repository.observeContents("item-1").first()).isEmpty()

    repository.saveContents("item-1", listOf(prologue, one, oneA))
    assertThat(repository.observeContents("item-1").first()).containsExactly(prologue, one, oneA)
    assertThat(repository.observeContents("item-2").first()).isEmpty()

    repository.saveContents("item-1", listOf(one))
    assertThat(repository.observeContents("item-1").first()).containsExactly(one)
  }

  @Test
  fun entryAt_isTheLastEntryStartedBy() {
    val contents = listOf(prologue, one, oneA)

    assertThat(contents.entryAt(0.05f)).isEqualTo(prologue)
    assertThat(contents.entryAt(0.5f)).isEqualTo(one)
    assertThat(listOf(one).entryAt(0.05f)).isNull()
  }
}
