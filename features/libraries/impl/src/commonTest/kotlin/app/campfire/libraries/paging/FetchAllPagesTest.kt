package app.campfire.libraries.paging

import app.campfire.network.PagedResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FetchAllPagesTest {

  private fun page(page: Int, data: List<Int>, total: Int, pageSize: Int = 2) = PagedResponse(
    data = data,
    page = page,
    limit = pageSize,
    total = total,
    offset = page * pageSize,
  )

  @Test
  fun `accumulates all pages until exhaustion`() = runTest {
    val pages = listOf(
      page(0, listOf(1, 2), total = 5),
      page(1, listOf(3, 4), total = 5),
      page(2, listOf(5), total = 5),
    )

    val result = fetchAllPages { p -> Result.success(pages[p]) }

    assertEquals(listOf(1, 2, 3, 4, 5), result.getOrThrow())
  }

  @Test
  fun `single page returns immediately`() = runTest {
    var calls = 0
    val result = fetchAllPages { p ->
      calls++
      Result.success(page(p, listOf(1, 2), total = 2))
    }

    assertEquals(listOf(1, 2), result.getOrThrow())
    assertEquals(1, calls)
  }

  @Test
  fun `empty library returns empty list`() = runTest {
    val result = fetchAllPages { p ->
      Result.success(page(p, emptyList(), total = 0))
    }

    assertEquals(emptyList(), result.getOrThrow())
  }

  @Test
  fun `page failure propagates as failure`() = runTest {
    val boom = IllegalStateException("boom")
    val result = fetchAllPages<Int> { p ->
      if (p == 0) {
        Result.success(page(0, listOf(1, 2), total = 10))
      } else {
        Result.failure(boom)
      }
    }

    assertTrue(result.isFailure)
    assertEquals(boom, result.exceptionOrNull())
  }

  @Test
  fun `page cap bounds a misbehaving server`() = runTest {
    var calls = 0
    // total never satisfied by returned data -> nextPage never null
    val result = fetchAllPages(maxPages = 3) { p ->
      calls++
      Result.success(page(p, listOf(p), total = Int.MAX_VALUE, pageSize = 1))
    }

    assertEquals(3, calls)
    assertEquals(listOf(0, 1, 2), result.getOrThrow())
  }
}
