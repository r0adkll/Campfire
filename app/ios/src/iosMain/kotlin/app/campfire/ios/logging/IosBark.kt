package app.campfire.ios.logging

import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.darwin.OS_LOG_DEFAULT
import platform.darwin.OS_LOG_TYPE_DEBUG
import platform.darwin.OS_LOG_TYPE_DEFAULT
import platform.darwin.OS_LOG_TYPE_ERROR
import platform.darwin.OS_LOG_TYPE_FAULT
import platform.darwin.OS_LOG_TYPE_INFO
import platform.darwin.__dso_handle
import platform.darwin._os_log_internal

object IosBark : Heartwood.Bark {

  @OptIn(ExperimentalForeignApi::class)
  override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: () -> String) {
    _os_log_internal(
      __dso_handle.ptr,
      OS_LOG_DEFAULT,
      priority.asIosLogType(forceInfo = true),
      message(priority, tag, message),
    )
  }

  private fun message(priority: LogPriority, tag: String?, message: () -> String): String {
    return if (tag.isNullOrBlank()) {
      "${priority.short}: ${message()}"
    } else {
      "${priority.short}/$tag: ${message()}"
    }
  }

  private fun LogPriority.asIosLogType(forceInfo: Boolean = false): UByte = when (this) {
    LogPriority.VERBOSE -> OS_LOG_TYPE_DEFAULT.takeIf { !forceInfo } ?: OS_LOG_TYPE_INFO
    LogPriority.DEBUG -> OS_LOG_TYPE_DEBUG.takeIf { !forceInfo } ?: OS_LOG_TYPE_INFO
    LogPriority.INFO -> OS_LOG_TYPE_INFO
    LogPriority.WARN -> OS_LOG_TYPE_FAULT
    LogPriority.ERROR -> OS_LOG_TYPE_ERROR
  }

  private fun Extras.flatten(): Array<Any?> {
    return entries
      .map { (key, value) -> "$key: $value" }
      .toTypedArray()
  }
}
