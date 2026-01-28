package app.campfire.core.settings

import app.campfire.core.settings.SortDisplayMode.Mode

enum class AuthorSortMode(
  override val storageKey: String,
  val networkKey: String,
  override val mode: Mode,
) : EnumSetting, SortDisplayMode {
  AuthorFL("author-first-last", "name", Mode.Alphabetical),
  AuthorLF("author-last-first", "lastFirst", Mode.Alphabetical),
  NumberOfBooks("number-of-books", "numBooks", Mode.Numerical),
  AddedAt("added-at", "addedAt", Mode.Numerical),
  UpdatedAt("updated-at", "updatedAt", Mode.Numerical)
  ;

  companion object : EnumSettingProvider<AuthorSortMode> {
    val Default = AuthorFL

    override fun fromStorageKey(key: String?): AuthorSortMode {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
