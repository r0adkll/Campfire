// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.core.model.Library
import app.campfire.core.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LibrarySwitcherTest {

  private val books = library("books", "Audiobooks", order = 0)
  private val podcasts = library("podcasts", "Podcasts", order = 1)
  private val radio = library("radio", "Radio dramas", order = 2)
  private val libraries = listOf(books, podcasts, radio)

  @Test
  fun `hidden with a single library`() {
    assertNull(librarySwitcherModel(books, listOf(books)) {})
  }

  @Test
  fun `hidden until the current library loads`() {
    assertNull(librarySwitcherModel(null, libraries) {})
  }

  @Test
  fun `hidden when the current library is not in the list`() {
    assertNull(librarySwitcherModel(radio, listOf(books, podcasts)) {})
  }

  @Test
  fun `shown with several libraries`() {
    assertNotNull(librarySwitcherModel(books, libraries) {})
  }

  @Test
  fun `swiping up steps forward and holds at the ends`() {
    assertEquals(0, swipeTargetIndex(startIndex = 0, dragY = -10f, stepPx = 32f, count = 3))
    assertEquals(1, swipeTargetIndex(startIndex = 0, dragY = -40f, stepPx = 32f, count = 3))
    assertEquals(2, swipeTargetIndex(startIndex = 0, dragY = -70f, stepPx = 32f, count = 3))
    assertEquals(2, swipeTargetIndex(startIndex = 0, dragY = -500f, stepPx = 32f, count = 3))
  }

  @Test
  fun `swiping down steps back and holds at the ends`() {
    assertEquals(1, swipeTargetIndex(startIndex = 2, dragY = 40f, stepPx = 32f, count = 3))
    assertEquals(0, swipeTargetIndex(startIndex = 2, dragY = 500f, stepPx = 32f, count = 3))
  }

  @Test
  fun `tapping opens the menu and picking a library selects it`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput { click() }
    onNodeWithText("Podcasts").performClick()

    assertEquals(listOf(podcasts), selections)
    onNodeWithText("Podcasts").assertDoesNotExist()
  }

  @Test
  fun `picking the current library does nothing`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput { click() }
    onNodeWithText("Audiobooks").performClick()

    assertTrue(selections.isEmpty())
  }

  @Test
  fun `swiping up selects the next library on release`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput {
      down(center)
      moveBy(Offset(0f, -20.dp.toPx()))
      moveBy(Offset(0f, -20.dp.toPx()))
      up()
    }

    assertEquals(listOf(podcasts), selections)
  }

  @Test
  fun `a swipe starting above the visible chip still switches`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    // The touch box is the pill's height; the 40dp chip is centered in it
    chip().performTouchInput {
      down(Offset(centerX, 3.dp.toPx()))
      moveBy(Offset(0f, -20.dp.toPx()))
      moveBy(Offset(0f, -20.dp.toPx()))
      up()
    }

    assertEquals(listOf(podcasts), selections)
  }

  @Test
  fun `a slanted swipe still switches`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput {
      down(center)
      moveBy(Offset(-12.dp.toPx(), -16.dp.toPx()))
      moveBy(Offset(-12.dp.toPx(), -24.dp.toPx()))
      up()
    }

    assertEquals(listOf(podcasts), selections)
  }

  @Test
  fun `a mostly sideways drag does nothing`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput {
      down(center)
      moveBy(Offset(-30.dp.toPx(), -10.dp.toPx()))
      moveBy(Offset(0f, -40.dp.toPx()))
      up()
    }

    assertTrue(selections.isEmpty())
    onNodeWithText("Podcasts").assertDoesNotExist()
  }

  @Test
  fun `swiping down from the first library selects nothing`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput {
      down(center)
      moveBy(Offset(0f, 20.dp.toPx()))
      moveBy(Offset(0f, 60.dp.toPx()))
      up()
    }

    assertTrue(selections.isEmpty())
  }

  @Test
  fun `long press then sliding onto a library and releasing selects it`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput { down(center) }
    mainClock.advanceTimeBy(LongPress)

    // The menu lives in a popup, so compare window positions rather than root bounds
    val chip = chip().fetchSemanticsNode().boundsInWindow
    val target = onNodeWithText("Radio dramas").fetchSemanticsNode().boundsInWindow
    chip().performTouchInput {
      moveBy(target.center - chip.center)
      up()
    }

    assertEquals(listOf(radio), selections)
  }

  @Test
  fun `long press released in place leaves the menu open`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput { down(center) }
    mainClock.advanceTimeBy(LongPress)
    chip().performTouchInput { up() }

    onNodeWithText("Radio dramas").performClick()
    assertEquals(listOf(radio), selections)
  }

  @Test
  fun `long press with a wobble does not pick the row under the finger`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = podcasts, onSelected = { selections += it }) }

    chip().performTouchInput { down(center) }
    mainClock.advanceTimeBy(LongPress)
    chip().performTouchInput {
      moveBy(Offset(1f, 1f))
      up()
    }

    assertTrue(selections.isEmpty())
    onNodeWithText("Radio dramas").assertExists()
  }

  @Test
  fun `long press then sliding off the menu closes it`() = runComposeUiTest {
    val selections = mutableListOf<Library>()
    setContent { Switcher(current = books, onSelected = { selections += it }) }

    chip().performTouchInput { down(center) }
    mainClock.advanceTimeBy(LongPress)
    onNodeWithText("Radio dramas").assertExists()
    chip().performTouchInput {
      moveBy(Offset(-200f, 0f))
      up()
    }

    onNodeWithText("Radio dramas").assertDoesNotExist()
    assertTrue(selections.isEmpty())
  }

  @Test
  fun `accessibility actions step through the libraries`() = runComposeUiTest {
    var current by mutableStateOf(podcasts)
    setContent { Switcher(current = current, onSelected = { current = it }) }

    chip().performCustomAction("Next library")
    assertEquals(radio, current)
    chip().performCustomAction("Previous library")
    chip().performCustomAction("Previous library")
    assertEquals(books, current)
  }

  private companion object {
    const val LongPress = 1_000L
  }

  private fun ComposeUiTest.chip(): SemanticsNodeInteraction =
    onNodeWithContentDescription("Library, ", substring = true)

  private fun SemanticsNodeInteraction.performCustomAction(label: String) {
    val actions = fetchSemanticsNode().config[SemanticsActions.CustomActions]
    actions.first { it.label == label }.action()
  }

  @Composable
  private fun Switcher(current: Library, onSelected: (Library) -> Unit) {
    val model = remember(current) { LibrarySwitcherModel(current, libraries, onSelected) }
    Box(Modifier.fillMaxSize().padding(top = 32.dp)) {
      LibrarySwitcher(model)
    }
  }

  private fun library(id: String, name: String, order: Int) = Library(
    id = id,
    name = name,
    displayOrder = order,
    icon = Library.Icon.Database,
    mediaType = MediaType.Book,
    provider = "audible",
    coverAspectRatio = 1,
    audiobooksOnly = false,
    createdAt = 0L,
    lastUpdate = 0L,
  )
}
