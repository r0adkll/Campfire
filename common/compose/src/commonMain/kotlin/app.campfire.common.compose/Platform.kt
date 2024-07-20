package app.campfire.common.compose

enum class Platform {
  ANDROID,
  IOS,
  DESKTOP,
}

expect val currentPlatform: Platform
