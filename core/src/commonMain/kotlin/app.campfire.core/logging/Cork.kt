package app.campfire.core.logging

/**
 * A custom interface to provide a central means of sharing a tag across various
 * different logging calls across various different files. This also provides a few
 * convenience methods for logging different priorities.
 */
interface Cork {

  /**
   * Custom tag to associate with all logging calls sent from this instance
   */
  val tag: String

  fun bark(
    priority: LogPriority,
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(tag, priority, throwable, extras, message)

  fun vbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(LogPriority.VERBOSE, throwable, extras, message)

  fun dbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(LogPriority.DEBUG, throwable, extras, message)

  fun ibark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(LogPriority.INFO, throwable, extras, message)

  fun wbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(LogPriority.WARN, throwable, extras, message)

  fun ebark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = bark(LogPriority.ERROR, throwable, extras, message)
}
