// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.settings

enum class GroupDisplayState(override val storageKey: String) : EnumSetting {
  List("list"),
  Grid("grid"),
  ;

  fun next(): GroupDisplayState = when (this) {
    List -> Grid
    Grid -> List
  }

  companion object : EnumSettingProvider<GroupDisplayState> {
    val Default get() = List

    override fun fromStorageKey(key: String?): GroupDisplayState {
      return entries.find { it.storageKey == key } ?: Default
    }
  }
}
