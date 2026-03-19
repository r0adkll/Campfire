package app.campfire.core

enum class Platform {
  ANDROID,
  IOS,
  DESKTOP,
}

expect val currentPlatform: Platform
