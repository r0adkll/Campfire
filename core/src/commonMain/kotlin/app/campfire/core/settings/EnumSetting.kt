// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.settings

interface EnumSetting {
  val storageKey: String
}

interface EnumSettingProvider<T> where T : Enum<T>, T : EnumSetting {
  fun fromStorageKey(key: String?): T
}
