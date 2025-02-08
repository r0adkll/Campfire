package app.campfire.core

import kotlin.experimental.ExperimentalNativeApi

actual val currentPlatform: Platform = Platform.IOS

@OptIn(ExperimentalNativeApi::class)
actual val isDebug: Boolean
  get() = kotlin.native.Platform.isDebugBinary
