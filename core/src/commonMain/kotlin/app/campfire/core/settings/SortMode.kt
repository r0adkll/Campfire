package app.campfire.core.settings

import app.campfire.core.settings.SortDisplayMode.Mode

enum class SortMode(
  override val storageKey: String,
  val networkKey: String,
  override val mode: Mode,
) : EnumSetting, SortDisplayMode {
  Title("title", "media.metadata.title", Mode.Alphabetical),
  AuthorFL("author-first-last", "media.metadata.authorName", Mode.Alphabetical),
  AuthorLF("author-last-first", "media.metadata.authorNameLF", Mode.Alphabetical),
  PublishYear("publish-year", "media.metadata.publishedYear", Mode.Numerical),
  AddedAt("added-at", "addedAt", Mode.Numerical),
  Size("size", "media.size", Mode.Numerical),
  Duration("duration", "media.duration", Mode.Numerical),
  ;

  companion object : EnumSettingProvider<SortMode> {
    val Default = AuthorFL

    override fun fromStorageKey(key: String?): SortMode {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
