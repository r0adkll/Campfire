// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.api.model

import app.campfire.core.model.ShelfType
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class HomeLayoutTest {

  @Test
  fun `default shelves put upcoming after discover`() {
    val shelves = defaultAvailableShelves(
      listOf(shelf("continue-listening"), shelf(ShelfIds.Discover), shelf("recent-series")),
    )

    assertThat(shelves.map { it.id }).containsExactly(
      "continue-listening",
      ShelfIds.Discover,
      ShelfIds.UpcomingReleases,
      "recent-series",
    )
  }

  @Test
  fun `default shelves put upcoming last without discover`() {
    val shelves = defaultAvailableShelves(listOf(shelf("continue-listening"), shelf("recent-series")))

    assertThat(shelves.map { it.id }).containsExactly(
      "continue-listening",
      "recent-series",
      ShelfIds.UpcomingReleases,
    )
  }

  @Test
  fun `without a saved layout home follows the available shelves`() {
    val layout = resolveHomeLayout(
      saved = null,
      available = listOf(available("a"), available("b"), available("client", defaultVisible = false)),
    )

    assertThat(layout.isCustomized).isFalse()
    assertThat(layout.shelves).containsExactly(
      layoutShelf("a", visible = true),
      layoutShelf("b", visible = true),
      layoutShelf("client", visible = false),
    )
    assertThat(layout.visibleShelves.map { it.id }).containsExactly("a", "b")
  }

  @Test
  fun `saved order and visibility win`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("b", visible = true), entry("a", visible = false)),
      available = listOf(available("a"), available("b")),
    )

    assertThat(layout.isCustomized).isTrue()
    assertThat(layout.shelves).containsExactly(
      layoutShelf("b", visible = true),
      layoutShelf("a", visible = false),
    )
  }

  @Test
  fun `a saved shelf that is not available keeps its slot but is not rendered`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("a"), entry("gone", label = "Gone"), entry("b")),
      available = listOf(available("a"), available("b")),
    )

    assertThat(layout.shelves).containsExactly(
      layoutShelf("a", visible = true),
      HomeLayoutShelf("gone", "Gone", visible = true, isAvailable = false),
      layoutShelf("b", visible = true),
    )
    assertThat(layout.visibleShelves.map { it.id }).containsExactly("a", "b")
  }

  @Test
  fun `a new shelf follows its nearest known server neighbor`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("c"), entry("a")),
      available = listOf(available("a"), available("new"), available("c")),
    )

    assertThat(layout.shelves.map { it.id }).containsExactly("c", "a", "new")
  }

  @Test
  fun `consecutive new shelves keep their server order`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("b"), entry("a")),
      available = listOf(available("a"), available("new1"), available("new2"), available("b")),
    )

    assertThat(layout.shelves.map { it.id }).containsExactly("b", "a", "new1", "new2")
  }

  @Test
  fun `a new shelf with no known neighbor before it goes to the top`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("b"), entry("a")),
      available = listOf(available("new"), available("a"), available("b")),
    )

    assertThat(layout.shelves.map { it.id }).containsExactly("new", "b", "a")
  }

  @Test
  fun `a new shelf uses its default visibility`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("a")),
      available = listOf(available("a"), available("client", defaultVisible = false)),
    )

    assertThat(layout.shelves).containsExactly(
      layoutShelf("a", visible = true),
      layoutShelf("client", visible = false),
    )
  }

  @Test
  fun `the current label replaces the saved one`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("a", label = "Old")),
      available = listOf(available("a", label = "New")),
    )

    assertThat(layout.shelves.map { it.label }).containsExactly("New")
  }

  @Test
  fun `duplicate saved entries collapse to the first`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("a", visible = false), entry("a", visible = true)),
      available = listOf(available("a")),
    )

    assertThat(layout.shelves).containsExactly(layoutShelf("a", visible = false))
  }

  @Test
  fun `entries round trip the layout`() {
    val layout = resolveHomeLayout(
      saved = listOf(entry("b", visible = false), entry("a")),
      available = listOf(available("a"), available("b")),
    )

    assertThat(layout.toEntries()).containsExactly(
      entry("b", visible = false),
      entry("a"),
    )
  }

  private fun shelf(id: String) = Shelf(id, label = id, total = 1, type = ShelfType.BOOK, order = 0)

  private fun available(id: String, label: String = id, defaultVisible: Boolean = true) =
    AvailableShelf(id, label, defaultVisible)

  private fun entry(id: String, visible: Boolean = true, label: String = id) = HomeLayoutEntry(id, visible, label)

  private fun layoutShelf(id: String, visible: Boolean) = HomeLayoutShelf(id, id, visible, isAvailable = true)
}
