package app.campfire.core.settings

enum class SortMode(
  override val storageKey: String,
  val networkKey: String,
) : EnumSetting {
  Title("title", "media.metadata.title"),
  AuthorFL("author-first-last", "media.metadata.authorName"),
  AuthorLF("author-last-first", "media.metadata.authorNameLF"),
  PublishYear("publish-year", "media.metadata.publishedYear"),
  AddedAt("added-at", "addedAt"),
  Size("size", "media.size"),
  Duration("duration", "media.duration"),
  ;

  companion object : EnumSettingProvider<SortMode> {
    val Default = AuthorFL

    override fun fromStorageKey(key: String?): SortMode {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
