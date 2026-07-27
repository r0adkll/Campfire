package app.campfire.settings

import app.campfire.core.model.MediaType
import app.campfire.settings.api.AndroidAutoCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidAutoCategoryTest {

  @Test
  fun `podcast libraries see home shows playlists and downloads`() {
    val podcastCategories = AndroidAutoCategory.entries
      .filter { it.isAvailableFor(MediaType.Podcast) }

    assertEquals(
      listOf(
        AndroidAutoCategory.Home,
        AndroidAutoCategory.Shows,
        AndroidAutoCategory.Playlists,
        AndroidAutoCategory.Downloads,
      ),
      podcastCategories,
    )
  }

  @Test
  fun `book libraries see the legacy categories and no shows tab`() {
    val bookCategories = AndroidAutoCategory.entries
      .filter { it.isAvailableFor(MediaType.Book) }

    assertEquals(
      listOf(
        AndroidAutoCategory.Home,
        AndroidAutoCategory.Series,
        AndroidAutoCategory.Authors,
        AndroidAutoCategory.Playlists,
        AndroidAutoCategory.Collections,
        AndroidAutoCategory.Downloads,
      ),
      bookCategories,
    )
  }

  @Test
  fun `every category supports at least one media type`() {
    AndroidAutoCategory.entries.forEach { category ->
      assertTrue(
        category.supportedMediaTypes.isNotEmpty(),
        "${category.name} supports no media types",
      )
    }
  }

  @Test
  fun `shows storage key round trips`() {
    assertEquals(
      AndroidAutoCategory.Shows,
      AndroidAutoCategory.fromStorageKey("shows"),
    )
  }

  @Test
  fun `storage keys are unique`() {
    val keys = AndroidAutoCategory.entries.map { it.storageKey }
    assertEquals(keys.size, keys.toSet().size)
  }
}
