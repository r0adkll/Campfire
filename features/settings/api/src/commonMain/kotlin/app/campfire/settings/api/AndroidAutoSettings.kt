package app.campfire.settings.api

import app.campfire.core.model.MediaType
import kotlinx.coroutines.flow.StateFlow

/**
 * Stable, platform-agnostic identifiers for top-level categories shown in Android Auto.
 * Storage keys are decoupled from the media IDs used by the Android Auto browser so the
 * browser can rename its internal IDs without invalidating user preferences.
 *
 * [supportedMediaTypes] restricts which library types a category applies to — the Android
 * Auto browser filters the root tabs by the active library's media type at browse time.
 */
enum class AndroidAutoCategory(
  val storageKey: String,
  val defaultGridLayout: Boolean = false,
  val alwaysVisible: Boolean = false,
  val pinned: Boolean = false,
  val supportedMediaTypes: Set<MediaType> = MediaType.entries.toSet(),
) {
  Home(storageKey = "home", defaultGridLayout = true, alwaysVisible = true, pinned = true),
  Shows(storageKey = "shows", defaultGridLayout = true, supportedMediaTypes = setOf(MediaType.Podcast)),
  Series(storageKey = "series", supportedMediaTypes = setOf(MediaType.Book)),
  Authors(storageKey = "authors", supportedMediaTypes = setOf(MediaType.Book)),
  Playlists(storageKey = "playlists"),
  Collections(storageKey = "collections", supportedMediaTypes = setOf(MediaType.Book)),
  Downloads(storageKey = "downloads"),
  ;

  fun isAvailableFor(mediaType: MediaType): Boolean = mediaType in supportedMediaTypes

  companion object {
    fun fromStorageKey(key: String): AndroidAutoCategory? =
      entries.firstOrNull { it.storageKey == key }
  }
}

data class AndroidAutoCategoryConfig(
  val category: AndroidAutoCategory,
  val visible: Boolean,
  val isGridLayout: Boolean,
)

interface AndroidAutoSettings {

  /**
   * The ordered list of category configs reflecting what is shown in Android Auto.
   *
   * Order, visibility, and grid-layout overrides are merged into this single view.
   * New categories introduced in future app versions appear at the tail with default values.
   */
  fun observeCategoryConfigs(): StateFlow<List<AndroidAutoCategoryConfig>>

  fun setCategoryVisible(category: AndroidAutoCategory, visible: Boolean)

  fun setCategoryGridLayout(category: AndroidAutoCategory, isGrid: Boolean)

  /**
   * Replace the current ordering. Any missing entries will be appended in enum order.
   */
  fun setCategoryOrder(order: List<AndroidAutoCategory>)
}
