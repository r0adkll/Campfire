// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.manage

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.libraries.api.BookDetailsUpdate
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.libraries.test.FakeLibraryItemRepository
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.api.match.QuickMatchResult
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class QuickMatcherTest {

  private val item = libraryItem(
    id = "item-1",
    libraryId = "fakeLibraryId",
    media = media(metadata = mediaMetadata(title = "The Martian")),
  )
  private val libraries = FakeLibraryRepository()
  private val matches = FakeQuickMatchRepository()
  private val items = FakeLibraryItemRepository()
  private val snackbarHost = SnackbarHostState()
  private val audible = MatchProvider("audible", "Audible.com")

  @Test
  fun `a quick match asks the library's own source, by its name`() = runTest {
    val source = matcher().sourceFor(item)

    assertThat(source).isEqualTo(audible)
  }

  @Test
  fun `a library the app hasn't cached asks the server's default source`() = runTest {
    libraries.libraries.value = emptyList()

    val source = matcher().sourceFor(item)

    assertThat(source).isEqualTo(MatchProvider("google", "Google Books"))
  }

  @Test
  fun `a source whose name can't be had goes by what the server calls it`() = runTest {
    matches.providersResult = Result.failure(IllegalStateException("offline"))

    val source = matcher().sourceFor(item)

    assertThat(source).isEqualTo(MatchProvider("audible", "audible"))
  }

  @Test
  fun `a match says it's under way, then sends what it may replace`() = runTest {
    val gate = CompletableDeferred<Unit>()
    matches.quickMatchGate = gate

    matcher().start(item, audible, QuickMatchChoice(replaceDetails = true, replaceCover = false), snackbarHost)

    awaitSnackbar("Matching The Martian on Audible.com…")
    assertThat(matches.quickMatches).containsExactly(
      FakeQuickMatchRepository.QuickMatch("item-1", "audible", replaceDetails = true, replaceCover = false),
    )

    gate.complete(Unit)

    awaitSnackbar("Audible.com had nothing new for The Martian")
  }

  @Test
  fun `changed details are counted and can be put back`() = runTest {
    val undo = BookDetailsUpdate(subtitle = "", genres = listOf("Fiction"))
    matches.quickMatchResult = Result.success(QuickMatchResult.Updated(undo, coverChanged = false))

    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    val told = awaitSnackbar("Changed 2 details of The Martian from Audible.com")
    assertThat(told.visuals.actionLabel).isEqualTo("Undo")

    told.performAction()

    awaitSnackbar("Put back the details of The Martian")
    assertThat(items.detailUpdates).containsExactly("item-1" to undo)
  }

  @Test
  fun `a changed cover is told beside the details, and alone`() = runTest {
    matches.quickMatchResult = Result.success(
      QuickMatchResult.Updated(BookDetailsUpdate(narrators = listOf("R. C. Bray")), coverChanged = true),
    )
    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    awaitSnackbar("Changed 1 detail and the cover of The Martian from Audible.com")
    snackbarHost.currentSnackbarData?.dismiss()

    matches.quickMatchResult = Result.success(QuickMatchResult.Updated(BookDetailsUpdate(), coverChanged = true))
    matcher().start(item, audible, QuickMatchChoice(replaceCover = true), snackbarHost)

    val coverOnly = awaitSnackbar("Changed the cover of The Martian from Audible.com")
    // Nothing to put back: a new cover takes the old one's place
    assertThat(coverOnly.visuals.actionLabel).isNull()
  }

  @Test
  fun `a change that couldn't be looked at is still told`() = runTest {
    matches.quickMatchResult = Result.success(QuickMatchResult.Updated(undo = null, coverChanged = false))

    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    assertThat(awaitSnackbar("Matched The Martian with Audible.com").visuals.actionLabel).isNull()
  }

  @Test
  fun `nothing found says where it looked`() = runTest {
    matches.quickMatchResult = Result.success(QuickMatchResult.NoMatch)

    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    awaitSnackbar("No match for The Martian on Audible.com")
  }

  @Test
  fun `a refused match says to ask the admin, and a failed one to try again`() = runTest {
    matches.quickMatchResult = Result.failure(ItemChangeNotAllowedException())
    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    awaitSnackbar("Your account isn't allowed to change this. Ask your server's admin.")
    snackbarHost.currentSnackbarData?.dismiss()

    matches.quickMatchResult = Result.failure(IllegalStateException("offline"))
    matcher().start(item, audible, QuickMatchChoice(), snackbarHost)

    awaitSnackbar("Couldn't match The Martian. Check your connection and try again.")
  }

  private fun TestScope.matcher() = quickMatcher(libraries, matches, items, backgroundScope)

  /** Waits for the snackbar saying [message]: its words load off the test's own thread */
  private suspend fun awaitSnackbar(message: String): SnackbarData = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      while (snackbarHost.currentSnackbarData?.visuals?.message != message) delay(10)
      checkNotNull(snackbarHost.currentSnackbarData)
    }
  }
}
