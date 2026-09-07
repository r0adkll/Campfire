// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core

enum class Platform {
  ANDROID,
  IOS,
  DESKTOP,
}

expect val currentPlatform: Platform

inline fun <T> forPlatform(
  android: () -> T,
  ios: () -> T,
  desktop: () -> T,
): T = when (currentPlatform) {
  Platform.ANDROID -> android()
  Platform.IOS -> ios()
  Platform.DESKTOP -> desktop()
}

inline fun <T> forPlatform(
  mobile: () -> T,
  desktop: () -> T,
): T = when (currentPlatform) {
  Platform.ANDROID,
  Platform.IOS,
  -> mobile()

  Platform.DESKTOP -> desktop()
}
