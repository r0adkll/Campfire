package app.campfire.settings.api

sealed class ThemeKey(val storageKey: String) {
  data object Tent : ThemeKey("tent")
  data object Rucksack : ThemeKey("rucksack")
  data object WaterBottle : ThemeKey("water_bottle")
  data object Forest : ThemeKey("forest")
  data object Mountain : ThemeKey("mountain")
  data object LifeFloat : ThemeKey("life_float")
  data object Dynamic : ThemeKey("dynamic")

  data class Custom(
    val id: String,
  ) : ThemeKey("custom__$id")

  companion object {
    fun from(storageKey: String): ThemeKey {
      return when (storageKey) {
        Tent.storageKey -> Tent
        Rucksack.storageKey -> Rucksack
        WaterBottle.storageKey -> WaterBottle
        Forest.storageKey -> Forest
        Mountain.storageKey -> Mountain
        LifeFloat.storageKey -> LifeFloat
        Dynamic.storageKey -> Dynamic
        else -> if (storageKey.startsWith("custom__")) {
          Custom(storageKey.removePrefix("custom__"))
        } else {
          throw IllegalArgumentException("Unknown theme key: $storageKey")
        }
      }
    }
  }
}
