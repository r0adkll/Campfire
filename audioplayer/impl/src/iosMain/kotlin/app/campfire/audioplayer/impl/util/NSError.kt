package app.campfire.audioplayer.impl.util

import platform.Foundation.NSError

fun NSError.asDebugString(): String {
  return """
    NSError(
      code = $code,
      domain = $domain,
      localizedDescription = $localizedDescription,
      localizedFailureReason = ${localizedFailureReason()},
      localizedRecoverySuggestion = $localizedRecoverySuggestion,
      localizedRecoveryOptions = [
        ${localizedRecoveryOptions?.joinToString(separator = "\n") { it.toString() }}
      ],
      userInfo = {
        ${userInfo.entries.joinToString(separator = "\n") { "${it.key} = ${it.value}," }}
      }
    )
  """.trimIndent()
}
