// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.browse

import kotlin.test.Test
import kotlin.test.assertEquals

class PaginateTest {

  private val items = (1..10).toList()

  @Test
  fun `first page returns the first pageSize items`() {
    assertEquals(listOf(1, 2, 3), items.paginate(page = 0, pageSize = 3))
  }

  @Test
  fun `middle page slices from the right offset`() {
    assertEquals(listOf(4, 5, 6), items.paginate(page = 1, pageSize = 3))
  }

  @Test
  fun `last page returns the partial remainder`() {
    assertEquals(listOf(10), items.paginate(page = 3, pageSize = 3))
  }

  @Test
  fun `page past the end returns empty`() {
    assertEquals(emptyList(), items.paginate(page = 4, pageSize = 3))
  }

  @Test
  fun `page size larger than the list returns everything`() {
    assertEquals(items, items.paginate(page = 0, pageSize = Int.MAX_VALUE))
  }

  @Test
  fun `huge page and pageSize does not overflow`() {
    assertEquals(emptyList(), items.paginate(page = 2, pageSize = Int.MAX_VALUE))
  }

  @Test
  fun `non-positive page size disables paging`() {
    assertEquals(items, items.paginate(page = 3, pageSize = 0))
    assertEquals(items, items.paginate(page = 0, pageSize = -1))
  }

  @Test
  fun `negative page returns empty`() {
    assertEquals(emptyList(), items.paginate(page = -1, pageSize = 3))
  }

  @Test
  fun `empty list always pages to empty`() {
    assertEquals(emptyList(), emptyList<Int>().paginate(page = 0, pageSize = 5))
  }
}
