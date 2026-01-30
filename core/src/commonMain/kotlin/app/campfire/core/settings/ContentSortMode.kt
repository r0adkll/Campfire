package app.campfire.core.settings

import app.campfire.core.settings.SortDisplayMode.Mode

data class SortNetworkKey(
  val libraryItemSortKey: String,
  val seriesSortKey: String = libraryItemSortKey,
  val authorSortKey: String = libraryItemSortKey,
)

enum class ContentSortMode(
  override val storageKey: String,
  val networkKey: SortNetworkKey,
  override val mode: Mode,
) : EnumSetting, SortDisplayMode {
  Title("title", "media.metadata.title", Mode.Alphabetical),
  AuthorFL(
    "author-first-last",
    SortNetworkKey("media.metadata.authorName", "", "name"),
    Mode.Alphabetical,
  ),
  AuthorLF(
    "author-last-first",
    SortNetworkKey("media.metadata.authorNameLF", "", "lastFirst"),
    Mode.Alphabetical,
  ),
  PublishYear("publish-year", "media.metadata.publishedYear", Mode.Numerical),
  Size("size", "media.size", Mode.Numerical),
  Duration(
    "duration",
    SortNetworkKey("media.duration", "totalDuration", ""),
    Mode.Numerical,
  ),
  AddedAt("added-at", "addedAt", Mode.Numerical),

  // Artist
  UpdatedAt("updated-at", "updatedAt", Mode.Numerical),
  NumberOfBooks("number-of-books", "numBooks", Mode.Numerical),

  // Series
  Name("name", "name", Mode.Alphabetical),
  LastBookAdded("last-book-added", "lastBookAdded", Mode.Normal),
  LastBookUpdated("last-book-updated", "lastBookUpdated", Mode.Normal),
  ;

  constructor(storageKey: String, networkKey: String, mode: Mode) : this(storageKey, SortNetworkKey(networkKey), mode)

  companion object {
    val LibraryItemSortMode = object : EnumSettingProvider<ContentSortMode> {
      val Default = AuthorFL
      override fun fromStorageKey(key: String?): ContentSortMode {
        return entries.find { it.storageKey == key } ?: Default
      }
    }

    val AuthorSortMode = object : EnumSettingProvider<ContentSortMode> {
      val Default = AuthorFL
      override fun fromStorageKey(key: String?): ContentSortMode {
        return entries.find { it.storageKey == key } ?: Default
      }
    }

    val SeriesSortMode = object : EnumSettingProvider<ContentSortMode> {
      val Default = Name
      override fun fromStorageKey(key: String?): ContentSortMode {
        return entries.find { it.storageKey == key } ?: Default
      }
    }
  }
}

object LibraryItemSortModes : SortModeConfig {
  override val availableModes = listOf(
    ContentSortMode.Title,
    ContentSortMode.AuthorFL,
    ContentSortMode.AuthorLF,
    ContentSortMode.PublishYear,
    ContentSortMode.Size,
    ContentSortMode.Duration,
    ContentSortMode.AddedAt,
  )
}

object SeriesSortModes : SortModeConfig {
  override val availableModes = listOf(
    ContentSortMode.Name,
    ContentSortMode.NumberOfBooks,
    ContentSortMode.AddedAt,
    ContentSortMode.LastBookAdded,
    ContentSortMode.LastBookUpdated,
    ContentSortMode.Duration,
  )
}

object AuthorSortModes : SortModeConfig {
  override val availableModes = listOf(
    ContentSortMode.AuthorFL,
    ContentSortMode.AuthorLF,
    ContentSortMode.NumberOfBooks,
    ContentSortMode.AddedAt,
    ContentSortMode.UpdatedAt,
  )
}
