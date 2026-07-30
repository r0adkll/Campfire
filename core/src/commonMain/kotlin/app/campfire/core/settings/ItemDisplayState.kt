// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.settings

enum class ItemDisplayState(override val storageKey: String) : EnumSetting {
  List("list"),
  Grid("grid"),
  GridDense("grid_dense"),
  ;

  companion object : EnumSettingProvider<ItemDisplayState> {
    val Default get() = Grid

    override fun fromStorageKey(key: String?): ItemDisplayState {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
