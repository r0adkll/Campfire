package app.campfire.crashreporting.impl

import app.campfire.core.logging.LogRedaction

/**
 * A sanitized mirror of a throwable whose message (or a cause's/suppressed's message)
 * contained a user's server URL. The original exception type is embedded in the message
 * and the stack trace is copied frame-for-frame, so Crashlytics grouping — which keys on
 * stack frames — is unaffected; only the message text changes.
 */
class RedactedException(message: String) : Exception(message)

private const val MAX_CHAIN_DEPTH = 20

/**
 * Returns this throwable untouched when no message in its chain needs redaction (the
 * common case — plain NPEs etc. keep their real type in the Crashlytics dashboard),
 * otherwise a [RedactedException] mirror of the entire chain.
 */
fun Throwable.redactedCopyOrSelf(): Throwable {
  return if (isDirty()) redactedCopy() else this
}

private fun Throwable.isDirty(depth: Int = 0): Boolean {
  if (depth > MAX_CHAIN_DEPTH) return false
  val message = message
  if (message != null && LogRedaction.redact(message) != message) return true
  if (suppressed.any { it.isDirty(depth + 1) }) return true
  return cause?.isDirty(depth + 1) ?: false
}

private fun Throwable.redactedCopy(depth: Int = 0): Throwable {
  val copy = RedactedException(
    buildString {
      append(this@redactedCopy.javaClass.name)
      this@redactedCopy.message?.let { append(": ").append(LogRedaction.redact(it)) }
    },
  )
  copy.stackTrace = stackTrace
  if (depth < MAX_CHAIN_DEPTH) {
    cause?.let { copy.initCause(it.redactedCopy(depth + 1)) }
    suppressed.forEach { copy.addSuppressed(it.redactedCopy(depth + 1)) }
  }
  return copy
}
