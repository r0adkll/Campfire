package app.campfire.core.settings

enum class SortDirection(override val storageKey: String) : EnumSetting {
  Ascending("asc"),
  Descending("desc"),
  ;

  fun flip(): SortDirection = when (this) {
    Ascending -> Descending
    Descending -> Ascending
  }

  companion object : EnumSettingProvider<SortDirection> {
    val Default = Ascending

    override fun fromStorageKey(key: String?): SortDirection {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
