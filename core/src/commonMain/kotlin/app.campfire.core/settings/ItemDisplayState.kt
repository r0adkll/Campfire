package app.campfire.core.settings

enum class ItemDisplayState(override val storageKey: String) : EnumSetting {
  List("list"),
  Grid("grid"),
  ;

  companion object : EnumSettingProvider<ItemDisplayState> {
    override fun fromStorageKey(key: String?): ItemDisplayState {
      return entries.find { it.storageKey == key } ?: List
    }
  }
}
