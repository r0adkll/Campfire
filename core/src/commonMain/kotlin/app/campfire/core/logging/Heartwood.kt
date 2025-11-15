package app.campfire.core.logging

typealias Extras = Map<String, String>

class Heartwood private constructor() {

  interface Bark {
    fun log(priority: LogPriority, tag: String?, extras: Extras?, message: String)
    fun log(priority: LogPriority, tag: String?, extras: Extras?, message: String, throwable: Throwable?) =
      log(priority, tag, extras, messageWithStacktrace(throwable, message))
  }

  companion object : Bark {
    private val barks = mutableListOf<Bark>()

    fun grow(bark: Bark) {
      barks.add(bark)
    }

    fun shrink(bark: Bark) {
      barks.remove(bark)
    }

    override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: String) {
      barks.forEach { it.log(priority, tag, extras, message) }
    }

    override fun log(priority: LogPriority, tag: String?, extras: Extras?, message: String, throwable: Throwable?) {
      barks.forEach { it.log(priority, tag, extras, message, throwable) }
    }
  }
}

enum class LogPriority(val priority: Int, val short: String) {
  VERBOSE(2, "V"),
  DEBUG(3, "D"),
  INFO(4, "I"),
  WARN(5, "W"),
  ERROR(6, "E"),
}

/**
 * Bark out a message into the world for all to hear.
 */
inline fun Any.bark(
  priority: LogPriority = LogPriority.DEBUG,
  /**
   * If provided, the log will use this tag instead of the simple class name of `this` at the call
   * site.
   */
  tag: String? = null,
  throwable: Throwable? = null,
  extras: Extras? = null,
  message: () -> String,
) {
  val tagOrCaller = tag ?: outerClassSimpleNameInternalOnlyDoNotUseKThxBye()
  Heartwood.log(priority, tagOrCaller, extras, message(), throwable)
}

/**
 * An overload for logging that does not capture the calling code as tag. This should only
 * be used in standalone functions where there is no `this`.
 * @see logcat above
 */
inline fun bark(
  tag: String,
  priority: LogPriority = LogPriority.DEBUG,
  throwable: Throwable? = null,
  extras: Extras? = null,
  message: () -> String,
) {
  Heartwood.log(priority, tag, extras, message(), throwable)
}

fun messageWithStacktrace(t: Throwable?, msg: String): String {
  var message = msg
  if (t != null) {
    message += "\n${t.stackTraceToString()}"
  }
  return message
}

@PublishedApi
internal fun Any.outerClassSimpleNameInternalOnlyDoNotUseKThxBye(): String? {
  val fullClassName = this::class.qualifiedName ?: return null
  val outerClassName = fullClassName.substringBefore('$')
  val simplerOuterClassName = outerClassName.substringAfterLast('.')
  return if (simplerOuterClassName.isEmpty()) {
    fullClassName
  } else {
    simplerOuterClassName.removeSuffix("Kt")
  }
}
