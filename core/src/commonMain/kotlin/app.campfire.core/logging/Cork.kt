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

  /**
   * Flag to enable or disable all logging calls sent from this instance
   */
  val enabled: Boolean get() = true

  fun bark(
    priority: LogPriority,
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(tag, priority, throwable, extras, message) else Unit

  fun vbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(LogPriority.VERBOSE, throwable, extras, message) else Unit

  fun dbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(LogPriority.DEBUG, throwable, extras, message) else Unit

  fun ibark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(LogPriority.INFO, throwable, extras, message) else Unit

  fun wbark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(LogPriority.WARN, throwable, extras, message) else Unit

  fun ebark(
    throwable: Throwable? = null,
    extras: Extras? = null,
    message: () -> String,
  ) = if (enabled) bark(LogPriority.ERROR, throwable, extras, message) else Unit
}
