// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.home

import app.campfire.home.api.model.HomeLayoutShelf
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class HomeLayoutEditsTest {

  private val layout = listOf(
    shelf("a"),
    shelf("hidden1", visible = false),
    shelf("b"),
    shelf("c"),
    shelf("hidden2", visible = false),
  )

  @Test
  fun `sections split shown from hidden in order`() {
    assertThat(layout.shownShelves.ids()).containsExactly("a", "b", "c")
    assertThat(layout.hiddenShelves.ids()).containsExactly("hidden1", "hidden2")
  }

  @Test
  fun `moving a shown shelf reorders the shown section`() {
    assertThat(layout.moveShown(from = 2, to = 0).ids())
      .containsExactly("c", "a", "b", "hidden1", "hidden2")
  }

  @Test
  fun `an out of range move leaves the order alone`() {
    assertThat(layout.moveShown(from = 0, to = 5).ids())
      .containsExactly("a", "b", "c", "hidden1", "hidden2")
  }

  @Test
  fun `moving by an offset is clamped to the shown section`() {
    assertThat(layout.moveShownBy("b", offset = 1).ids())
      .containsExactly("a", "c", "b", "hidden1", "hidden2")
    assertThat(layout.moveShownBy("a", offset = -1).ids())
      .containsExactly("a", "b", "c", "hidden1", "hidden2")
    assertThat(layout.moveShownBy("c", offset = 3).ids())
      .containsExactly("a", "b", "c", "hidden1", "hidden2")
  }

  @Test
  fun `showing a shelf adds it to the end of the shown section`() {
    val edited = layout.setVisible("hidden2", visible = true)

    assertThat(edited.ids()).containsExactly("a", "b", "c", "hidden2", "hidden1")
    assertThat(edited.shownShelves.ids()).containsExactly("a", "b", "c", "hidden2")
  }

  @Test
  fun `hiding a shelf moves it to the top of the hidden section`() {
    val edited = layout.setVisible("b", visible = false)

    assertThat(edited.shownShelves.ids()).containsExactly("a", "c")
    assertThat(edited.hiddenShelves.ids()).containsExactly("b", "hidden1", "hidden2")
  }

  @Test
  fun `editing an unknown shelf changes nothing`() {
    assertThat(layout.setVisible("missing", visible = true).ids())
      .containsExactly("a", "b", "c", "hidden1", "hidden2")
    assertThat(layout.moveShownBy("missing", offset = 1).ids())
      .containsExactly("a", "b", "c", "hidden1", "hidden2")
  }

  private fun List<HomeLayoutShelf>.ids() = map { it.id }

  private fun shelf(id: String, visible: Boolean = true) = HomeLayoutShelf(id, id, visible, isAvailable = true)
}
