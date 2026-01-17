package app.campfire.common.test.logging

import app.campfire.core.logging.Extras
import app.campfire.core.logging.Heartwood
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.LogPriority.DEBUG
import app.campfire.core.logging.LogPriority.ERROR
import app.campfire.core.logging.LogPriority.INFO
import app.campfire.core.logging.LogPriority.VERBOSE
import app.campfire.core.logging.LogPriority.WARN

object SystemBark : Heartwood.Bark {
  override fun log(
    priority: LogPriority,
    tag: String?,
    extras: Extras?,
    message: () -> String,
  ) {
    println(
      buildString {
        // Tag
        if (tag != null) {
          append("[$tag] ")
        }

        // Priority
        append(
          when (priority) {
            VERBOSE -> "V: "
            DEBUG -> "D: "
            INFO -> "I: "
            WARN -> "W: "
            ERROR -> "E: "
          },
        )

        append(message())
      },
    )
  }
}
