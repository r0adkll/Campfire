package app.campfire.core.settings

enum class SortMode(override val storageKey: String) : EnumSetting {
  Title("title"),
  AuthorFL("author-first-last"),
  AuthorLF("author-last-first"),
  PublishYear("publish-year"),
  AddedAt("added-at"),
  Size("size"),
  Duration("duration"),
  ;

  companion object : EnumSettingProvider<SortMode> {
    val Default = AuthorFL

    override fun fromStorageKey(key: String?): SortMode {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
